package test.main;

import domain.auction.repository.InMemoryRepository;
import domain.auction.service.AuctionServiceImpl;
import messaging.dispatchers.ArrayBlockingQueueDispatcher;
import messaging.dispatchers.CommandDispatcher;
import messaging.dispatchers.DisruptorDispatcher;
import messaging.dispatchers.LinkedBlockingQueueDispatcher;
import test.benchmarks.AuctionBenchmarks;
import test.benchmarks.BenchmarkBase;
import test.benchmarks.OverallLatencyBenchmark;
import test.util.BenchmarkResults;
import test.util.BenchmarkUtils;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class OverallLatencyTest {

    private static final int ITERATIONS = 10;
    private static final int BATCH_SIZE = 1000 * 1000;
    private static final int BUFFER_SIZE = 1024 * 1024;
    private static final DecimalFormat doubleFormatter = (DecimalFormat) NumberFormat.getIntegerInstance(Locale.US);

    public static void main(String[] args) {
        try {

            BenchmarkResults results = new BenchmarkResults();

            CommandDispatcher abq = new ArrayBlockingQueueDispatcher(new AuctionServiceImpl(new InMemoryRepository()), BUFFER_SIZE, null);

            runFor(abq, results);

            CommandDispatcher lbq = new LinkedBlockingQueueDispatcher(new AuctionServiceImpl(new InMemoryRepository()), BUFFER_SIZE, null);

            runFor(lbq, results);

            CommandDispatcher disruptor = new DisruptorDispatcher(new AuctionServiceImpl(new InMemoryRepository()), BUFFER_SIZE, null);

            runFor(disruptor, results);

            results.ExportToCSV("Overall_Latency");

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static void runFor(CommandDispatcher dispatcher, BenchmarkResults results) throws Exception {

        BenchmarkBase test = AuctionBenchmarks.OVERALL_LATENCY.getInstance();

        byte[] placeBid = BenchmarkUtils.PrepareDispatcherAndGetPlaceBidCommand(dispatcher);

        List<String> column = new ArrayList<>();

        column.add(dispatcher.getClass().getSimpleName());

        //Warm up
        test.run(dispatcher, placeBid, BATCH_SIZE);

        for (int iteration = 0; iteration < ITERATIONS; iteration++) {

            test.run(dispatcher, placeBid, BATCH_SIZE);

            double averageLatency = ((OverallLatencyBenchmark) test).getAverageLatency();

            column.add(doubleFormatter.format(averageLatency));
        }

        results.addColumn(column);

        dispatcher.shutdown();

        System.gc();
    }
}
