A project which provides a reactive stream view for filtering/throttling over kafka streams

It includes a filtering expression syntax and means for clients to supply desired rates (e.g. N messages / second) as well as a backpressure strategy.

e.g., A slow consumer may be only interested in getting 100 messages per second, but will want to process every second worth of data.

Another consumer may want 100 messages/second, and would be happy to drop messages:


For example, at 12:00:00 both consumers get their 100 messages, but then take a full minute to process them.

When each requests their next batch of messages a minute later (e.g. the time now is 12:01:00) :
 * the first consumer will get 100 messages from 12:00:01 to 12:00:02
 * the second consumer will get 100 messages from 12:01:00 to 12:01:01 as they always want the 'latest'
