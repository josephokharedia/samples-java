package io.serialized.samples.feed.order;

import io.serialized.samples.feed.client.Feed;
import io.serialized.samples.feed.client.FeedEntryHandler;

import static java.lang.String.format;

public class OrderFeedEntryHandler implements FeedEntryHandler {

  @Override
  public void handle(Feed.FeedEntry feedEntry) {
    System.out.println("Processing entry with sequenceNumber: " + feedEntry.sequenceNumber);

    for (Feed.FeedEntry.Event event : feedEntry.events) {
      switch (event.eventType) {
        case "OrderPlacedEvent": {
          System.out.println(format("An order with ID [%s] was placed by customer [%s]", event.data.get("orderId"), event.data.get("customerId")));
          break;
        }
        case "OrderPaidEvent": {
          System.out.println(format("The order with ID [%s] was paid, amountPaid: %s, amountLeft: %s", event.data.get("orderId"), event.data.get("amountPaid"), event.data.get("amountLeft")));
          break;
        }
        case "OrderShippedEvent": {
          System.out.println(format("The order with ID [%s] was shipped, trackingNumber: %s", event.data.get("orderId"), event.data.get("trackingNumber")));
          break;
        }
        case "OrderCancelledEvent": {
          System.out.println(format("The order with ID [%s] was cancelled, reason: %s", event.data.get("orderId"), event.data.get("reason")));
          break;
        }
        case "PaymentReceivedEvent": {
          System.out.println(format("The order with ID [%s] received payment: %s", event.data.get("orderId"), event.data.get("amountPaid")));
          break;
        }
        case "OrderFullyPaidEvent": {
          System.out.println(format("The order with ID [%s] is fully paid", event.data.get("orderId")));
          break;
        }
        default:
          System.out.println("Don't know how to handle events of type: " + event.eventType);
      }
    }
  }

}
