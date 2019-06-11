import tweepy
import csv
import json
import re
import nltk

#install natural lang toolkit to remove spacing, Punctuation
nltk.download('stopwords')
from nltk.corpus import stopwords

#credentials
consumer_key = "ujAZVtzyskdftDrxZbG0imX21"
consumer_secret = "NSeKr1Lv90mqQ3z5cwR21udEv8tpBpsAydO7o4dZztl5kunAnI"
access_key = "1045373297024794626-rBNydl1YVylgjcUjmOWwZfPwLfLTpi"
access_secret = "9DdlMKyfQpRRaKIiT813A63FWYG8doTq21R4GWZwIT5bs"
CountPerTweet = 100

auth = tweepy.OAuthHandler(consumer_key, consumer_secret)
auth.set_access_token(access_key, access_secret)
api = tweepy.API(auth)


#this function removes special characters from list of words
def Remove_Special_Char(tweettext):
    cleantext: str = re.sub('(@[A-Za-z0-9]+)|([^0-9A-Za-z \t])|(\w+:\/\/\S+)', ' ', tweettext)
    cleantext = cleantext.lower()
    stop_words = set(stopwords.words('english'))

    splitSentence = cleantext.split();

    filtered_sentence = [w for w in splitSentence if not w in stop_words]

    filtered_sentence = []

    for w in splitSentence:
        if w not in stop_words and w != 'rt':
            filtered_sentence.append(w)

    return filtered_sentence

#function to get tweets from twitter apis
def get_tweets(query):
    api = tweepy.API(auth)
    try:
        tweets = api.search(q=query, count=CountPerTweet)
    except tweepy.error.TweepError as e:
        tweets = json.loads(e.response.text)
    return tweets


queries = ["#TreatyDay", "#FightFor15", "#Rocktober", "#facebook", "#Accenture", "@realDonaldTrump", "@BarackObama",
           "\"Nova Scotia\"", "\"The New York Times\"", "\"California\"", "\"united States\"", "\"Canada\"",
           "\"Toronto\"", "\"scotia bank\""]

with open('tweets10.csv', 'w', encoding="utf-8") as outfile:
    writer = csv.writer(outfile)
    writer.writerow(['text','Sentiment Score','Analysis'])
    for query in queries:
        t = get_tweets(query)
        #for loop to iterate each tweet
        for tweet in t:
            sentimentscore=0
            #returns clean text
            cleanedtext = Remove_Special_Char(tweet.text)
            #iterate throuch each word in a tweet
            for tw in cleanedtext:
                #read words from lexicon file and compare to do sentiment analysis
                with open('d:/lexicons.csv') as f:
                    readCSV = csv.reader(f, delimiter=',')
                    for row in readCSV:
                        if tw == row[0]:
                            sentimentscore=int(sentimentscore)+int(row[1])

            #conditions to check if the tweet is positive, negative or neutral
            if sentimentscore > 0:
                writer.writerow([cleanedtext,sentimentscore,"Positive"])
            elif sentimentscore < 0:
                writer.writerow([cleanedtext, sentimentscore, "Negative"])
            else:
                writer.writerow([cleanedtext, sentimentscore, "Neutral"])