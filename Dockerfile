FROM hseeberger/scala-sbt

WORKDIR /usr/local/comicazi

COPY . /usr/local/comicazi

CMD ["sbt", "run"]
