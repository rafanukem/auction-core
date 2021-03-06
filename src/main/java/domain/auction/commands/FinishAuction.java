package domain.auction.commands;

import domain.auction.Auction;
import domain.auction.events.Event;

public class FinishAuction extends Command {

    public FinishAuction(String auctionId, long timestamp) {
        super(auctionId, timestamp);
    }

    @Override
    public Event accept(Auction auction) throws Exception {
        return auction.onCommand(this);
    }
}
