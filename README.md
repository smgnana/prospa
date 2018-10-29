# ftp-heroku

Downloads from FTP site and creates a Heroku site which provides a HTTP request.

## Setup

Run 

`mvn clean install`

`git push heroku master`

https://devcenter.heroku.com/articles/getting-started-with-java#deploy-the-app


## How to Invoke

### GET path http://localhost:8080/file/wav/<FilePathAsBase64Encoded>

Ex: http://localhost:8080/file/wav/LzExNzEvMjAxOC8xMC8wMS8tMTc1MzEwNzY3MV8wMTY2MzIxMC1hMzY0LWJhZTEtODQ1MS00YWRlZjRlOGJmMDkud2F2

You have to encode the filepath to Base 64 and suffix to URL.

It looks for Basic Auth heading. The Heading should be in the form of 

`Authorization: Basic userid:password`

Where userid and password are FTP credentials