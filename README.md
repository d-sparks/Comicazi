# Comicazi

A subscription service for the purchasing of rare comic books.

## Development notes:

Done or in progress:

* Using MongoDB because of familiarity, even though it's not the ideal tool.
* Using sbt and ensime, because that's what the internet told me to do.
* Using collosus/akka, jackson, mongo-scala-driver to make a simple REST API for POST, GET and DELETE methods for comic books and subscriptions (in progress).
* Using ScalaTest for unit tests (in progress).

Intentions:

* Build a fake brokerage service that, when somebody is notified, has a 75% chance of completing the sale and updating inventory correspondingly.
* Build a notification service; possibly a pub-sub type model or another endpoint within the same service, but definitely deployed in a separate Docker container and not sharing traffic from the main API.
* Use Docker for deployment.
* Make an integration test.
* Architecturally, have three Mongo instances in a replicaset for failover safety.
* Handle database re-elections gracefully.
* Have multiple copies of the service behind an HAProxy instance for scalability and robustness.

If I have time:

* Add Circle.ci to the github PR.
* Use Supervisor to keep the services up if they crash; ideally this would be on the dockerhosts, but I think in order to make the deployment butter smooth I may find a way to achieve this from within the container or have the multiple copies of the service check each other's health, and perform CPR if necessary.
* Make the notification service scalable to a large number of subscribers and decent flow of new comic books becoming available.
