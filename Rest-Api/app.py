import os
import json
from os import path as pth
from pytube import YouTube
from pydub import AudioSegment
from dejavu import Dejavu
from dejavu.recognize import FileRecognizer
from flask import Flask, request, abort, jsonify
from werkzeug.utils import secure_filename
from celery import Celery, states
from celery.exceptions import Ignore
from flask_sqlalchemy import SQLAlchemy
from sqlalchemy.dialects.mysql import MEDIUMINT
from marshmallow import Schema, fields, ValidationError, pre_dump
from flask_jwt_extended import create_access_token, get_jwt_identity, jwt_required, JWTManager

TMP_DOWNLOAD_FOLDER = '.tmp-download/'
TMP_UPLOAD_FOLDER = '.tmp-upload/'
DOWNLOAD_AUDIO_FORMAT = 'audio/webm'
ALLOWED_EXTENSIONS = set(['mp3', 'webm', '3gp', 'ogg'])
MEDIA_TYPES = ['television', 'movie', 'music']

def init_config(configpath):
    """ 
    Load config from a JSON file
    """
    try:
        with open(configpath) as f:
            config = json.load(f)
    except IOError as err:
        print("Cannot open configuration: %s. Exiting" % (str(err)))
    return config

config = init_config("CONFIG.json")

app = Flask(__name__)
app.config['UPLOAD_FOLDER'] = TMP_UPLOAD_FOLDER
app.config['CELERY_BROKER_URL'] = 'amqp://{0}:{1}@localhost:5672/vbooyah'.format(config['rabbitmq']['user'], config['rabbitmq']['passwd'])
app.config['CELERY_RESULT_BACKEND'] = 'db+mysql://{0}:{1}@{2}/dejavu'.format(config['database']['user'], config['database']['passwd'], config['database']['host'])
app.config['SQLALCHEMY_DATABASE_URI'] = 'mysql://{0}:{1}@{2}/dejavu'.format(config['database']['user'], config['database']['passwd'], config['database']['host'])
app.config['SQLALCHEMY_TRACK_MODIFICATIONS'] = True
app.config['JWT_SECRET_KEY'] = 'super-secret' #TODO: Generate using os
db  = SQLAlchemy(app)
djv = Dejavu(config)
jwt = JWTManager(app)
clry = Celery(app.name, backend=app.config['CELERY_RESULT_BACKEND'], broker=app.config['CELERY_BROKER_URL'])
clry.conf.update(app.config)

# create upload folders on app load
if (pth.isdir(TMP_UPLOAD_FOLDER) == False):
	print "Creating upload folder"
	os.mkdir(TMP_UPLOAD_FOLDER)

# SQLAlchemy models
class Users(db.Model):
	"""
	Users model.
	"""
	id = db.Column(db.Integer, primary_key=True, autoincrement=True)
	signature = db.Column(db.String(255), primary_key=True)

	def __init__(self, signature):
		"""
		Initialize class.
		"""
		self.signature = signature

class IndexedMedia(db.Model):
	"""
	Map existing songs table to a db Model.
	"""
	table = db.Table("songs", db.metadata, autoload=True, autoload_with=db.engine)
	__table__ = table
	id = table.c.song_id
	name = table.c.song_name

class Media(db.Model):
	"""
	Media model.
	"""
	id = db.Column(db.Integer, primary_key=True, autoincrement=True)
	name = db.Column(db.String(255), nullable=False)
	duration = db.Column(db.BigInteger, nullable=False)
	author = db.Column(db.String(255), nullable=False)
	mtype = db.Column(db.String(255), nullable=False)
	sid = db.Column(MEDIUMINT(unsigned=True), db.ForeignKey('songs.song_id'))

	def __init__(self, name, duration, author, mtype, sid):
		"""
		Initialize class.
		"""
		self.name = name
		self.duration = duration
		self.author = author
		self.mtype = mtype
		self.sid = sid

class Likes(db.Model):
	"""
	Likes model.
	"""
	media = db.Column(db.Integer, db.ForeignKey('media.id'), primary_key=True) #circumvent int primary key req
	user = db.Column(db.Integer, db.ForeignKey('users.id'), primary_key=True)
	seconds = db.Column(db.JSON, nullable=False)

	def __init__(self, user, media, seconds):
		"""
		Initialize class.
		"""
		self.user = user
		self.media = media
		self.seconds = seconds

class Dislikes(db.Model):
	"""
	Dislikes model.
	"""
	media = db.Column(db.Integer, db.ForeignKey('media.id'), primary_key=True) #circumvent int primary key req
	user = db.Column(db.Integer, db.ForeignKey('users.id'), primary_key=True)
	seconds = db.Column(db.JSON, nullable=False)

	def __init__(self, user, media, seconds):
		"""
		Initialize class.
		"""
		self.user = user
		self.media = media
		self.seconds = seconds

db.create_all()

# marshmallow schemas
def userSignatureValidator(data):
	"""
	Validate user signature.
	"""
	user = Users.query.filter_by(signature=data).first()
	if user != None:
		raise ValidationError('Please provide another signature.')

def mediaTypeValidator(data):
	"""
	Validate media type.
	"""
	if data and data.lower() not in MEDIA_TYPES:
		raise ValidationError('Mtype is invalid.')

def emptyLikesValidator(data):
	"""
	Ensure likes is not empty.
	"""
	if not data or len(data) == 0:
		raise ValidationError('Seconds cannot be empty.')

class UserSchema(Schema):
	"""
	User serialization/deserialization schema.
	"""
	signature = fields.Str(required=True, load_only=True, validate=userSignatureValidator)

class MediaSchema(Schema):
	"""
	Media serialization/deserialization schema.
	"""
	id = fields.Int(required=True, dump_only=True)
	name = fields.Str(required=True)
	author = fields.Str(required=True)
	duration = fields.Int(default=0, missing=0)
	mtype = fields.Str(required=True, validate=mediaTypeValidator)
	url = fields.Url(load_only=True)
	indexed = fields.Method('check_indexed', dump_only=True)

	def check_indexed(self, media):
		"""
		Return Boolean indicator if media is indexed.
		"""
		return not media.sid == None

class LikesDislikesSchema(Schema):
	"""
	Likes & dislikes serialization/deserialization schema.
	"""
	#Discard seconds out of timer window
	user = fields.Int(required=True, dump_only=True)
	media = fields.Int(required=True, dump_only=True)
	seconds = fields.List(fields.Int(), required=True, validate=emptyLikesValidator)

	@pre_dump
	def process_json(self, data):
		"""
		Convert json string to array before
		passing it to dump().
		"""
		data.seconds = json.loads(data.seconds)
		return data

user_schema = UserSchema()
media_schema = MediaSchema()
media_list_schema = MediaSchema(many=True)
user_likes_schema = LikesDislikesSchema()
media_likes_schema = LikesDislikesSchema(many=True)

def allowed_file(filename):
	return '.' in filename and filename.rsplit('.', 1)[1].lower() in ALLOWED_EXTENSIONS

#TODO: Increase no. workers
@clry.task
def testInstall():
	"""
	Test installation.
	"""
	return "Hello " + get_jwt_identity()

@clry.task(bind=True)
def fingerprintMedia(self, media):
	"""
	Fingerprint and add a given media.
	"""
	url = media.get("url", None)
	sid = None
	if url != None: #fingerprint
		try:
			yt = YouTube(url)
		except Exception as err:
			return {"data":{"msg":"Media unavailable."}, "code": 500}
		media['duration'] = int(yt.length)
		stream_list = yt.streams.filter(only_audio=True).all()
		stream = None
		for i in xrange(0, len(stream_list)):
			if stream_list[i].mime_type == DOWNLOAD_AUDIO_FORMAT:
				stream = stream_list[i]
				break;
		if stream == None:
			return {"data":{"msg":"Media stream unavailable."}, "code": 500}
		if (pth.isdir(TMP_DOWNLOAD_FOLDER) == False):
			os.mkdir(TMP_DOWNLOAD_FOLDER)
		try:
			filepath = stream.download(TMP_DOWNLOAD_FOLDER)
			sid = djv.fingerprint_file(filepath)
			#os.remove(filepath) # rmv file after use
		except Exception as err:
			return {"data":{"msg":"Unable to index media."}, "code": 500}
		if sid <= 0:
			return {"data":{"msg":"Media already exists."}, "code": 409}
	row = Media(name=media['name'], duration=media['duration'], author=media['author'], mtype=media['mtype'], sid=sid)
	db.session.add(row)
	db.session.commit()
	db.session.refresh(row)
	return {"data": media_schema.dump(row), "code": 201}

@clry.task(bind=True)
def recognizeMedia(self, filepath):
	#TODO: Use sth better than filenames
	result = {}
	try:
		song = djv.recognize(FileRecognizer, filepath)
		media = Media.query.filter_by(sid=song['song_id']).first()
		if media:
			print song['song_id']
			result = {
					"id": media.id,
					"offset": song['offset_seconds'],
					"duration": media.duration,
					"match_time": song['match_time']
				}
	except Exception as e:
		return {"data":{"msg":"Recognition failed."}, "code": 500}
	if not song:
		return {"data":{"msg":"Media not found."}, "code": 404}
	return {"data":result, "code": 200}
		
@app.route('/hello', methods=['GET'])
@jwt_required
def helloApi():
	"""
	Installation test.
	"""
	asynctask = testInstall.apply()
	if asynctask.ready() and asynctask.successful():
		return jsonify({"msg": "Success!"})
	abort("Bad installation", 500)

@app.route('/register', methods=['POST'])
def registerApi():
	"""
	Add a user to the database.
	"""
	if not request.is_json or request.get_json() == None:
		abort(400, "Json data not provided.")
	json_data = request.get_json()
	try:
		data = user_schema.load(json_data)
	except ValidationError as err:
		return jsonify(err.messages), 400
	user = Users(signature=data['signature'])
	db.session.add(user)
	db.session.commit()
	db.session.refresh(user)
	token = create_access_token(identity=data['signature'], expires_delta=False)
	return jsonify({"uid":user.id, "access_token":token})

@app.route('/media', methods=['GET','POST'])
@jwt_required
def mediaApi():
	"""
	Add & retrieve media.
	"""
	if request.method == 'GET':
		media_list = Media.query.order_by(Media.name).all()
		data = media_list_schema.dump(media_list)
		return jsonify(data), 200
	elif request.method == 'POST':
		if not request.is_json or request.get_json() == None:
			abort(400, "Json data not provided.")
		json_data = request.get_json()
		try:
			data = media_schema.load(json_data)
		except ValidationError as err:
			return jsonify(err.messages), 400
		asynctask = fingerprintMedia.delay(data) #TODO: Ensure celery always recieves task b4 returning
		return jsonify({"uuid": asynctask.task_id}), 202

@app.route('/media/status/<uuid:sid>', methods=['GET'])
@jwt_required
def fingerprintStatusApi(sid):
	"""
	Retrieve the status of a fingerprinting task.
	"""
	fingerprinter = fingerprintMedia.AsyncResult(sid) #TODO: Handle sids that don't exist
	if fingerprinter.ready():
		if fingerprinter.successful():
			result = fingerprinter.get()
			return jsonify(result['data']), result['code']
		if fingerprinter.failed():
			return abort(500, "Error indexing media.")
	return jsonify({"uuid": str(sid)}), 202

@app.route('/media/<int:mid>', methods=['GET'])
def mediaItemApi(mid):
	"""
	Retrieve the details for the media mid.
	"""
	media = Media.query.get(mid)
	if not media:
		abort(404, "Media not found.")
	return jsonify(media_schema.dump(media))

@app.route('/media/recognize', methods=['POST'])
@jwt_required
def mediaRecognitionApi():
	"""
	Retrieve the resource id, name, author
	and time index of a sampled media.
	"""
	#TODO: Improve recognition
	if 'file' not in request.files:
		abort(400, "No file.")
	file = request.files['file']
	if file.filename == '':
		abort(400, "No selected file")
	if file and allowed_file(file.filename):
		filename = secure_filename(file.filename)
		filepath = pth.join(app.config['UPLOAD_FOLDER'], filename)
		file.save(filepath)
		asynctask = recognizeMedia.delay(filepath)
		return jsonify({"uuid": asynctask.task_id}), 202
	abort(400, "Bad request")

@app.route('/media/recognize/status/<uuid:sid>', methods=['GET'])
@jwt_required
def recognitionStatusApi(sid):
	"""
	Retieve the status of a recognition activity.
	"""
	recognizer = recognizeMedia.AsyncResult(sid)
	if recognizer.ready():
		if recognizer.successful():
			result = recognizer.get()
			return jsonify(result['data']), result['code']
		if recognizer.failed():
			return abort(500, "Error recognizing media.")
	return jsonify({"uuid": str(sid)}), 202

@app.route('/media/<int:mid>/likes', methods=['GET'])
@app.route('/media/<int:mid>/dislikes', methods=['GET'])
@jwt_required
def mediaLikesApi(mid):
	"""
	Retrieve list of user likes for a media.
	"""
	try:
		if Media.query.get(mid) == None:
			abort(404, "Media not found.")
	except Exception as e:
		abort(404, "Ratings not found.")
	likes = (str(request.url_rule).split("/")[-1] == "likes")
	if likes:
		rating = Likes.query.filter_by(media=mid).order_by(Likes.user).all()
	else:
		rating = Dislikes.query.filter_by(media=mid).order_by(Dislikes.user).all()
	if not rating:
		jsonify([])
	return jsonify(media_likes_schema.dump(rating))

@app.route('/media/<int:mid>/likes/<int:uid>', methods=['GET', 'POST', 'PUT', 'DELETE'])
@app.route('/media/<int:mid>/dislikes/<int:uid>', methods=['GET', 'POST', 'PUT', 'DELETE'])
@jwt_required
def userLikesApi(mid, uid):
	"""
	Retrieve, add & modify the user likes
	for a particular media.
	"""
	try:
		user = Users.query.filter_by(signature=get_jwt_identity()).first()
		if user == None or user.id != uid:
			raise Exception
	except Exception as e:
		abort(401)
	try:
		if Media.query.get(mid) == None:
			raise Exception
	except Exception as e:
		abort(404, "Media not found.")
	likes = (str(request.url_rule).split("/")[-2] == "likes")
	if likes:
		qresult = Likes.query.filter_by(user=uid, media=mid)
	else:
		qresult = Dislikes.query.filter_by(user=uid, media=mid)
	existingRatings = qresult.first()
	if request.method == 'GET':
		if not existingRatings:
			return jsonify({})
		return jsonify(user_likes_schema.dump(existingRatings))
	elif request.method == 'DELETE':
		if not existingRatings:
			abort(404, "Ratings not found.")
		qresult.delete()
		db.session.commit()
		return jsonify({"success": True})
	else:
		if not request.is_json or request.get_json() == None:
			abort(400, "Json data not provided.")
		json_data = request.get_json()
		try:
			data = user_likes_schema.load(json_data)
		except ValidationError as err:
			return jsonify(err.messages), 400
	 	if request.method == 'POST':
			if existingRatings:
				abort(409, "User ratings exists for media.")
			else: #create
	 			if likes:
					newRatings = Likes(user=uid, media=mid, seconds=json.dumps(data["seconds"]))
				else:
					newRatings = Dislikes(user=uid, media=mid, seconds=json.dumps(data["seconds"]))
				db.session.add(newRatings)
				db.session.commit()
				return jsonify({"user": uid, "media": mid, "seconds": data["seconds"]}), 201
		elif request.method == 'PUT':
			if not existingRatings:
				abort(404, "Ratings not found.")
			else: #modify
				existingRatings.seconds = json.dumps(data["seconds"])
				db.session.commit()
				return jsonify({"user": uid, "media": mid, "seconds": data["seconds"]}), 200