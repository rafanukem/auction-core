package domain.auction.service;

import domain.auction.commands.Command;

public interface AuctionService {

    void processCommand(Command command) throws Exception;

    void replayHistory();

    void close();

}
