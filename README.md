# Java samples for Serialized

Sample code showing how to work with the Serialized API:s. 

## Get your free API-keys

[Sign up](https://serialized.io/) and login to get your free API-keys to [Serialized](https://serialized.io).

## Clone and build using Maven

```
git clone git@github.com:serialized-io/samples-java.git
mvn clean install
```

## Event Feed API

Start by opening one terminal window and copy/paste the commands below.

```
export SERIALIZED_ACCESS_KEY=<your-access-key>
export SERIALIZED_SECRET_ACCESS_KEY=<your-secret_access-key>
mvn -pl event-feed exec:java -Dexec.mainClass="io.serialized.samples.feed.FeedTest"
```

You now have a running java process with an active subscription to the feed `order`.
You are now ready to store some events so keep reading!

## Event Sourcing API

Open a second terminal window and copy/paste the commands below.

```
export SERIALIZED_ACCESS_KEY=<your-access-key>
export SERIALIZED_SECRET_ACCESS_KEY=<your-secret_access-key>
mvn -pl event-sourcing exec:java -Dexec.mainClass="io.serialized.samples.aggregate.order.OrderTest"
```

You should se some output similar to the one below indicating the events were successfully stored in your cloud space 
at Serialized. Go back to the first terminal window and you should notice that the order events were successfully processed!

```
Placing order: OrderId[id=f8fa9a84-d55c-4b2d-a121-3a149a933b28]
Event(s) successfully saved
Loading aggregate with ID: OrderId[id=f8fa9a84-d55c-4b2d-a121-3a149a933b28]
Cancelling order: OrderId[id=f8fa9a84-d55c-4b2d-a121-3a149a933b28]
Event(s) successfully saved
Placing order: OrderId[id=8220a09a-cdf5-4e93-a93d-9f6cbff065d9]
Event(s) successfully saved
Loading aggregate with ID: OrderId[id=8220a09a-cdf5-4e93-a93d-9f6cbff065d9]
Paying order: OrderId[id=8220a09a-cdf5-4e93-a93d-9f6cbff065d9]
Event(s) successfully saved
Loading aggregate with ID: OrderId[id=8220a09a-cdf5-4e93-a93d-9f6cbff065d9]
Shipping order: OrderId[id=8220a09a-cdf5-4e93-a93d-9f6cbff065d9]
Event(s) successfully saved
```

