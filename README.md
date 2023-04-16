# Simple example with http/2 zio-quartz-h2 server/client with jsoniter-scala json codec accessing Chat GPT API.

quartz-h2 http2 server.<br>
https://github.com/ollls/zio-quartz-h2

JSON library.<br>
https://github.com/plokhotnyuk/jsoniter-scala

## Commands:

- "sbt run"<br>
http POST english text to https://127.0.0.1:8443/token.

 - "sbt assembly"<br>
"java -jar qh2-http-run.jar"

 - "sbt test"<br>
Make sure yo have proper path for
  val FOLDER_PATH = "...."
  val BIG_FILE = "img_0278.jpeg"



