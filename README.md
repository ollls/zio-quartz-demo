# Simple example with http/2 zio-quartz-h2 server/client with jsoniter-scala json codec accessing Chat GPT API.

This is a template project for zio-quartz-h2
- onConnect/OnDisconnect responsible for client connections wih H2Client ZIO service.
- You can have ZIO-tests for your project. Just add/replace test in /test folder.
- Please, use provided JSON codec or repace with your own.

quartz-h2 http2 server.<br>
https://github.com/ollls/zio-quartz-h2

JSON library.<br>
https://github.com/plokhotnyuk/jsoniter-scala

## Commands:

- ```sbt run```
http POST english text to https://127.0.0.1:8443/token.

 - ```sbt assembly```
 ```java -jar qh2-http-run.jar```

 - ```sbt test```
Make sure you have proper path for
```scala
  val FOLDER_PATH = "...."
  val BIG_FILE = "img_0278.jpeg"
```
Options for logging level.
```
sbt "run --debug"
sbt "run --error"
sbt "run --off"
```



