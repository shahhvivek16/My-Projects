from elasticsearch import helpers, Elasticsearch
import csv

elasticCredential = Elasticsearch("https://portal-ssl953-33.bmix-dal-yp-f288ae4a-a549-4ef8-bc29-cb618bcb4200.1794435291.composedb.com:58079/", http_auth=('admin','XHOZYDFNJYBEGREC'))

with open('d:/FinalSentimentAnalysis.csv') as f:
    reader = csv.DictReader(f)
    helpers.bulk(elasticCredential, reader, index='sentimentanalysis-index', doc_type='my-type')