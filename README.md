# Comicazi

A subscription service for the purchasing of rare comic books.

### API

#### POST /comics

1. Request body is json which must meet the Comic schema.
2. Find all SearchPatterns, and for each, create a PendingQuery.
3. Create a NotificationJob.
4. Insert the comic into inventory.

If there is failure at 1, respond with code 4xx. If there is failure at steps 2 or 3, respond with code 5xx. If there is a failure at step 4, there is a problem: we might be notifying on a comic we fail to account for in inventory. I will come back to this, but some thoughts:

- Delete the NotificationJob and resopnd with 5xx. Then, change the notification worker such that, upon picking up a job, it sleeps for twice the duration of the datastore's timeout, then checks if the NotificationJob still exists.
- Retry the comic book insert a few times.

Until the database is vertically sharded, these are unlikely to be helpful. We'd not likely be unable to insert a comic but be able to delete a NotificationJob, as this instance is likely in an unhealthy state with respect to the datastore.

#### POST /subscriptions

Insert the subscription, appending its SearchPattern.

Indexes:

- A unique index on (email,body)
- An index on (email)
- Indexes on (searchpattern,field) for each Comic field

#### POST /completetransaction

If the comic is not in inventory:

- refund customer
- datastore.remove({email,comic}, "notifications")
- respond "We're sorry, but we cannot complete this transaction. We'll notify you again if this comic becomess available in the future."

If the comic is in inventory:

- make a record of the sale
- remove the comic from inventory

### Notification system

A *SearchPattern* is just a string of alphabetically concatenated, distinct strings which are each Comic field names. There are 2^n possible SearchPatterns, where n is the number of fields on the Comic schema.


A *NotificationJob* is of the form
```scala
comic: String // alphabetized json of the comic
handler: Int // which instance will handle the job
```
This table should have a unique index on the comic field. Axiomatic is that the NotificationJob will continue to exist until all of its corresponding PendingQuery and PendingNotification's are fulfilled (and removed).

A *PendingQuery* is of the form
```scala
querystring: String // The actual db query
comic: String // Alphabetized json of the comic
```
This table should be uniquely indexed on (comic,querystring).

A *PendingNotification* is of the form
```scala
email: String // Who to notify
comic: String // What the comic book is
```
This table should be uniquely indexed on (email,comic).

A *Notification* is exactly the same as a PendingNotification, but in a different table. The table is used to track historical notifications, but also, to ensure that notification emails aren't sent twice.

#### The handler of NotificationJobs

Each handler sits in an infinite loop and is assigned a positive integer number, n. The loop looks like this in sceudocode:

```
datastore.findFirst({"handler":n}, "notificationjobs")
success, NJ: NotificationJob =>
  while {
    datastore.findFirst({"comic": NJ.comic}, "pendingqueries")
    success, Q: PendingQuery =>
      datastore.find({pattern: Q.querystring}, "subscription")
      for S: Subscription a result {
        datastore.put({S.email, Q.comic}, "pendingnotifications")
      }
      datastore.remove(Q, "pendingqueries")
    success: None =>
      while {
        datastore.findFirst(
          {"comic": NJ.comic},
          "pendingnotifications"
        )
        success, P: PendingNotification =>
          http POST /notify body: {comic, email}
          success: datastore.remove(P, "pendingnotifications")
          failure: continue
        success, None: =>
          // yay we're doned!
          datastore.remove(NJ, "notificationjobs")
        failure => continue
    failure => continue
  }
failure:
  Sleep for some amount of time
```
If my reasoning is correct, the above loop can be restarted at any time and resumed. If a job handler becomes unhealthy, the dream would be to have its siblings detect this and change the `handler` field of all NotificationJobs to a random healthy handler.

##### Notes on choices / Ideas for future

A PendingQuery's querystring will include "querypattern": "...". I am estimating very few people will want to be notified of every single comic book, I think that the "querypattern": "" will not have many results at all. In all other cases: In practice, I am basing the entire approach around the hypothesis that querypatterns will be fairly varied, and that the datastore will choose the best (querypattern,comicfield) so that the query is of reasonable size.

In order to guarantee this, my thinking was to keep track of how many subscriptions have each query pattern, and if a querypattern becomes extremely large (say, 10^4 or 10^5 entries), making a new QueryPattern by affixing an increasing integer to the bloated pattern, and making new subscriptions that would produce the bloated pattern, use the new one.

Another thought that I had was to calculate the diversity of a comic field in some way, and hinting on (querypattern,mostdiversefieldinthepattern). The diversity could be some heuristic like, the maximum frequency over its values divided by the sum of the frequency of its values (lower is better). So, if 99% of people who care about *publisher* are picking Marvel, that would be a not diverse field. But we'd likely see more variety in *series*: even there is a lot of interest in "The Amazing Spiderman", it would probably be balanced by a variety of other titles, and thus more diverse. An even coarser heuristic would just be the number of distinct values, but that doesn't matter much if one particular value is extremely dominant.


