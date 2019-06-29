B00yah-rest installation instructions
=====================================

1. Install MySQL server.
2. Change `CONFIG.json.template` file into `CONFIG.json`.
3. Enter MySQL username & password into `CONFIG.json`.
4. Install conda.
5. Create a conda environment that uses python 2.7: `conda create -n <envname> python=2.7`.
6. Activate the coda environment: `conda activate <envname>`.
7. Add conda-forge channel: `conda config --add channels conda-forge`.
8. Install conda packages: `conda install --file req-conda.txt`.
9. Install pip packages for this env: `pip install -r req-pip.txt`.
10. Insall RabbitMQ server.
11. Start RabbitMQ server in background: `sudo rabbitmq-server -detached`.
12. Set RabbitMQ username and password: `sudo rabbitmqctl add_user <username> <password>`.
13. Add virtual host to RabbitMQ: `sudo rabbitmqctl add_vhost vbooyah`.
14. Set permissions for virtual host: `sudo rabbitmqctl set_permissions -p vbooyah <username> ".*" ".*" ".*"`.
15. Enter RabbitMQ username & password into `CONFIG.json`.
16. Just create a new MySQL database _rabbitmq_.
17. Set flask app environment variable: `export FLASK_APP=app.py`.
18. Run flask: `flask run`.
19. Run celery worker in another terminal: `celery -A app.celery worker --loglevel=info`.
20. Install postman or any http client.
21. Send a GET to http://127.0.0.1:5000/hello. You should get `{"message": "Success!"}`.

If you have any questions, please contact me.