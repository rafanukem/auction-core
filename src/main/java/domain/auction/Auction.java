package domain.auction;

import domain.auction.commands.*;
import domain.auction.events.*;
import domain.auction.exceptions.AuctionCancelledException;
import domain.auction.exceptions.AuctionEndedException;
import domain.auction.exceptions.AuctionNotStartedException;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class Auction {
    private final UUID id;
    private static final List<Method> handleMethods = Arrays.stream(Auction.class.getMethods()).filter(m -> m.getName().contains("onCommand")).collect(Collectors.toList());
    private static final List<Method> applyMethods = Arrays.stream(Auction.class.getMethods()).filter(m -> m.getName().contains("onEvent")).collect(Collectors.toList());
    private UUID auctioneerId;
    private UUID itemId;
    private UUID currentWinnerId;
    private double currentWinningBid = 0;
    private AuctionState state;

    public Auction(UUID id) {
        this.id = id;
    }

    public Event handle(Command c) throws Exception {
        for (Method m : handleMethods)
            if (m.getParameterTypes()[0].equals(c.getClass()))
                return (Event) m.invoke(this, c);

        return null;
    }

    public void apply(Event e) throws Exception {
        for (Method m : applyMethods)
            if (m.getParameterTypes()[0].equals(e.getClass()))
                m.invoke(this, e);
    }

    public AuctionCancelled onCommand(CancelAuction cmd) throws Exception {

        if (state == AuctionState.CREATED) {
            throw new AuctionNotStartedException("Auction hasn't started");
        }

        if (state == AuctionState.CANCELLED) {
            throw new AuctionCancelledException("Auction has already been cancelled");
        }

        if (state == AuctionState.ENDED) {
            throw new AuctionEndedException("Auction has already ended");
        }

        return new AuctionCancelled(cmd.getAuctionId(), cmd.getTimestamp());
    }

    public AuctionCreated onCommand(CreateAuction cmd) {

        return new AuctionCreated(cmd.getAuctionId(), cmd.getTimestamp(), cmd.getAuctioneerId(), cmd.getItemId());
    }

    public AuctionEnded onCommand(EndAuction cmd) throws Exception {

        if (state == AuctionState.CREATED) {
            throw new AuctionNotStartedException("Auction hasn't started");
        }

        if (state == AuctionState.CANCELLED) {
            throw new AuctionCancelledException("Auction has already been cancelled");
        }

        if (state == AuctionState.ENDED) {
            throw new AuctionEndedException("Auction has already ended");
        }

        return new AuctionEnded(cmd.getAuctionId(), cmd.getTimestamp());
    }

    public BidPlaced onCommand(PlaceBid cmd) throws Exception {

        if (state == AuctionState.CREATED) {
            throw new AuctionNotStartedException("Auction hasn't started");
        }

        if (state == AuctionState.CANCELLED) {
            throw new AuctionCancelledException("Auction has already been cancelled");
        }

        if (state == AuctionState.ENDED) {
            throw new AuctionEndedException("Auction has already ended");
        }

        return new BidPlaced(cmd.getAuctionId(), cmd.getTimestamp(), cmd.getBidderId(), cmd.getAmount());
    }

    public AuctionStarted onCommand(StartAuction cmd) throws Exception {

        List<Event> result = new ArrayList<>();

        if (state == AuctionState.STARTED) {
            throw new AuctionNotStartedException("Auction has already started");
        }

        if (state == AuctionState.CANCELLED) {
            throw new AuctionCancelledException("Auction has already been cancelled");
        }

        if (state == AuctionState.ENDED) {
            throw new AuctionEndedException("Auction has already ended");
        }

        return new AuctionStarted(cmd.getAuctionId(), cmd.getTimestamp());
    }

    public void onEvent(AuctionCancelled evt) {
        state = AuctionState.CANCELLED;
    }

    public void onEvent(AuctionCreated evt) {
        auctioneerId = evt.getAuctioneerId();
        itemId = evt.getItemId();
        state = AuctionState.CREATED;
    }

    public void onEvent(AuctionEnded evt) {
        state = AuctionState.ENDED;
    }

    public void onEvent(BidPlaced evt) {
        if (evt.getAmount() > currentWinningBid) {
            currentWinnerId = evt.getBidderId();
            currentWinningBid = evt.getAmount();
        }
    }

    public void onEvent(AuctionStarted evt) {
        state = AuctionState.STARTED;
    }

    public enum AuctionState {
        CREATED,
        STARTED,
        CANCELLED,
        ENDED
    }
}