package com.mycompany.fxausd;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.time.*;
import java.util.*;
import java.util.Base64;
import javax.swing.*;
import javax.net.ssl.SSLSocketFactory;
import com.sun.net.httpserver.HttpServer;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.xy.DefaultOHLCDataset;
import org.jfree.data.xy.OHLCDataset;
import org.jfree.data.xy.OHLCDataItem;

public class Fxausd {

    private static final Logger logger = LoggerFactory.getLogger(Fxausd.class);

    static class Candle {

        long time;
        double open, high, low, close, volume;

        Candle(double o, double h, double l, double c, double v) {
            this.time = 0;
            open = o;
            high = h;
            low = l;
            close = c;
            volume = v;
        }

        Candle(long t, double o, double h, double l, double c, double v) {
            time = t;
            open = o;
            high = h;
            low = l;
            close = c;
            volume = v;
        }
    }

    static final String[] FEATURE_NAMES = {
        "closeDist", "rsi", "ma20", "ma50", "momentum", "volatility", "macd", "bbPercent",
        "bbWidth", "stochK", "atr", "atrPct", "cci", "body", "range", "bodyRatio",
        "upperWick", "lowerWick", "emaDiff", "trendSlope"
    };
    static final int NUM_FEATURES = FEATURE_NAMES.length;

    // Institutional Elite Thresholds (Optimized for QILH world-wide intelligence)
    static final double ELITE_MIN_SMC_CONFLUENCE = 0.65;
    static final double ELITE_MIN_VOLUME_SPIKE = 1.05;
    static final double ELITE_MIN_ML_PROBABILITY = 0.65;
    static final double ELITE_MIN_RR_RATIO = 2.0;

    public static class MarketIntelligence {

        public String bias = "NEUTRAL";
        public String session = "IDLE";
        public double volatility = 0.0;
        public boolean bos = false;
        public boolean choch = false;
        public double liquidityScore = 0.0;
        public String setupQuality = "LOW";
        public double imbalanceRatio = 0.0;
        public double sentimentScore = 0.5;
        public double dominanceIndex = 1.0;
        public double trendStrength = 0.0;
        public double volumeIntensity = 0.0;
        public double institutionalDisplacement = 0.0;
        public double institutionalPressure = 0.0;
        public String heartbeat = "NORMAL";
    }

    public static MarketIntelligence currentIntel = new MarketIntelligence();

    // Institutional Bank-Grade Constants
    static final double BANK_MIN_ACCURACY = 0.90;
    static final double WORLD_BANK_RESERVE_PERCENT = 0.01; // Conservative 1% risk per setup
    static final double LIQUIDITY_POOL_THRESHOLD = 0.85;
    static final double ORDER_FLOW_INTENSITY = 1.25;

    // Live decision thresholds
    static final double MIN_ML_CONFIDENCE = 0.62;
    static final double MIN_SMC_CONFIDENCE = 0.65;
    static final double MIN_SIGNAL_STRENGTH = 45.0;
    static final double MIN_COMBINED_SCORE = 40.0;
    static final double MIN_RISK_REWARD = 1.5;
    static final double DEBUG_MIN_ML_CONFIDENCE = 0.65;
    static final double DEBUG_MIN_SMC_CONFIDENCE = 0.70;
    static final double DEBUG_MIN_SIGNAL_STRENGTH = 55.0;
    static final int DEBUG_PREVIEW_MAX = 3;
    static final int MAX_LIVE_SIGNALS = 5;
    static final String LIVE_SIGNAL_MOMENTUM_ENV = "LIVE_SIGNAL_MOMENTUM";
    static final String LIVE_SIGNAL_MOMENTUM_ARG_PREFIX = "--min-signal-momentum=";
    static final String LIVE_ATR_PERCENT_ENV = "LIVE_ATR_PERCENT";
    static final String LIVE_ATR_PERCENT_ARG_PREFIX = "--min-atr-percent=";
    static final String LIVE_SKIP_RSI_CHOP_ENV = "LIVE_SKIP_RSI_CHOP";
    static final String LIVE_SKIP_HTF_STRUCTURE_ENV = "LIVE_SKIP_HTF_STRUCTURE";
    static final String LIVE_SKIP_BREAKOUT_RANGE_ENV = "LIVE_SKIP_BREAKOUT_RANGE";
    static final String LIVE_FORCE_SIGNAL_ENV = "LIVE_FORCE_SIGNAL";
    static final double GOLD_MIN_CONFIDENCE = 0.88;
    static final double GOLD_MIN_VOLUME_MULTIPLIER = 1.10;
    static final double GOLD_MIN_PRICE_VOLUME_CORRELATION = 0.15;
    static final double INSTITUTIONAL_MIN_ORDERBLOCK = 0.60;
    static final double INSTITUTIONAL_MIN_LIQUIDITY = 0.40;
    static final double INSTITUTIONAL_MIN_FVG = 0.20;
    static final double INSTITUTIONAL_MIN_PRICE_VOLUME_CORRELATION = 0.12;
    static double MIN_SIGNAL_MOMENTUM = parseDoubleEnv(LIVE_SIGNAL_MOMENTUM_ENV, 0.0003);
    static double MIN_ATR_PERCENT = parseDoubleEnv(LIVE_ATR_PERCENT_ENV, 0.0001);
    static final double RSI_OVERSOLD_ZONE = 35.0;
    static final double RSI_OVERBOUGHT_ZONE = 65.0;
    static final double RSI_CHOP_MIN = 30.0;
    static final double RSI_CHOP_MAX = 70.0;
    static final double MAX_MEAN_REVERSION_MOMENTUM = 0.025;
    static final double MEAN_REVERSION_OVERSOLD = 30.0;
    static final double MEAN_REVERSION_OVERBOUGHT = 70.0;
    static final double BREAKOUT_RSI_MIN = 50.0;
    static final double BREAKOUT_CONSOLIDATION_RANGE_PCT = 0.03;
    static final double BREAKOUT_ATR_MULTIPLIER = 0.4;
    static final int MIN_TRADE_SCORE = 5;
    static final int DEFAULT_ACTIVE_SESSION_START_HOUR_UTC = 7;
    static final int DEFAULT_ACTIVE_SESSION_END_HOUR_UTC = 21;
    static final String LIVE_ACTIVE_SESSION_START_ENV = "LIVE_ACTIVE_SESSION_START_UTC";
    static final String LIVE_ACTIVE_SESSION_END_ENV = "LIVE_ACTIVE_SESSION_END_UTC";
    static final java.util.List<String> PRIMARY_FX_SYMBOLS = Arrays.asList(
            "XAUUSD", "GBPUSD", "EURUSD", "BTCUSD", "XAGUSD"
    );

    private static String getBaseSymbol(String brokerSymbol) {
        if (brokerSymbol == null) {
            return "";
        }
        for (String base : PRIMARY_FX_SYMBOLS) {
            if (brokerSymbol.toUpperCase().startsWith(base)) {
                return base;
            }
        }
        return brokerSymbol.toUpperCase();
    }
    static final double DEFAULT_ACCOUNT_BALANCE = 10000.0;

    private static java.util.List<TradeSignal> selectTopUniqueSymbolSignals(java.util.List<TradeSignal> signals, int maxSignals) {
        java.util.List<TradeSignal> selected = new ArrayList<>();
        java.util.Set<String> usedSymbols = new java.util.HashSet<>();
        for (TradeSignal signal : signals) {
            if (selected.size() >= maxSignals) {
                break;
            }
            if (usedSymbols.contains(signal.symbol)) {
                continue;
            }
            selected.add(signal);
            usedSymbols.add(signal.symbol);
        }
        return selected;
    }

    public static boolean isForexMarketClosed() {
        ZonedDateTime nowUtc = ZonedDateTime.now(ZoneOffset.UTC);
        DayOfWeek day = nowUtc.getDayOfWeek();
        int hour = nowUtc.getHour();
        // Forex Market standard: Sunday 21:00 UTC to Friday 21:00 UTC
        if (day == DayOfWeek.FRIDAY && hour >= 21) {
            return true;
        }
        if (day == DayOfWeek.SATURDAY) {
            return true;
        }
        if (day == DayOfWeek.SUNDAY && hour < 21) {
            return true;
        }
        return false;
    }

    public static boolean isCrypto(String symbol) {
        if (symbol == null) return false;
        String s = symbol.toUpperCase();
        return s.contains("BTC") || s.contains("ETH") || s.contains("SOL") || s.contains("ADA") || 
               s.contains("DOGE") || s.contains("XRP") || s.contains("LTC") || s.contains("DOT") ||
               s.contains("PEPE") || s.contains("SHIB") || s.contains("AVAX");
    }


    private static boolean isWithinActiveForexSession() {
        int startHour = parseIntEnv(LIVE_ACTIVE_SESSION_START_ENV, DEFAULT_ACTIVE_SESSION_START_HOUR_UTC);
        int endHour = parseIntEnv(LIVE_ACTIVE_SESSION_END_ENV, DEFAULT_ACTIVE_SESSION_END_HOUR_UTC);
        ZonedDateTime nowUtc = ZonedDateTime.now(ZoneOffset.UTC);
        LocalTime currentTime = nowUtc.toLocalTime();

        if (startHour == DEFAULT_ACTIVE_SESSION_START_HOUR_UTC && endHour == DEFAULT_ACTIVE_SESSION_END_HOUR_UTC) {
            // Standard Active Institutional Sessions (UTC)
            LocalTime londonOpen = LocalTime.of(7, 0);
            LocalTime londonClose = LocalTime.of(16, 0);
            LocalTime nyOpen = LocalTime.of(12, 0);
            LocalTime nyClose = LocalTime.of(21, 0);

            return isTimeBetween(currentTime, londonOpen, londonClose)
                    || isTimeBetween(currentTime, nyOpen, nyClose);
        }

        LocalTime startTime = LocalTime.of(startHour, 0);
        LocalTime endTime = LocalTime.of(endHour, 0);
        return isTimeBetween(currentTime, startTime, endTime);
    }

    private static boolean isTimeBetween(LocalTime current, LocalTime start, LocalTime end) {
        if (start.isBefore(end)) {
            return !current.isBefore(start) && current.isBefore(end);
        }
        return !current.isBefore(start) || current.isBefore(end);
    }

    private static int parseIntEnv(String envKey, int fallback) {
        String value = System.getenv(envKey);
        if (value == null || value.isEmpty()) {
            return fallback;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private static double parseDoubleEnv(String envKey, double fallback) {
        String value = System.getenv(envKey);
        if (value == null || value.isEmpty()) {
            return fallback;
        }
        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private static String getTrainingAssetSymbol() {
        // Use the primary dataset asset for model persistence
        String asset = "EURUSD";
        String datasetPath = "data/eurusd.csv";
        if (new java.io.File(datasetPath).exists()) {
            return asset;
        }
        return asset;
    }

    private static String resolveModelPath(String assetSymbol, String fileName) {
        String normalized = assetSymbol == null ? "UNKNOWN" : assetSymbol.toUpperCase().replaceAll("[^A-Z0-9]", "_");
        java.io.File modelDir = new java.io.File("models");
        if (!modelDir.exists()) {
            modelDir.mkdirs();
        }
        return new java.io.File(modelDir, normalized + "_" + fileName).getPath();
    }

    private static double getAdaptiveMlThreshold(String regime, double pairPerfMult) {
        double threshold = MIN_ML_CONFIDENCE;
        if ("TRENDING".equals(regime)) {
            threshold += 0.02;
        } else if ("VOLATILE".equals(regime)) {
            threshold += 0.03;
        } else if ("ACCUMULATION".equals(regime) || "DISTRIBUTION".equals(regime)) {
            threshold += 0.01;
        }
        if (pairPerfMult < 0.95) {
            threshold += 0.03;
        }
        return Math.min(threshold, 0.85);
    }

    private static double getAdaptiveSmcThreshold(String regime, double pairPerfMult) {
        double threshold = MIN_SMC_CONFIDENCE;
        if ("TRENDING".equals(regime)) {
            threshold += 0.02;
        } else if ("VOLATILE".equals(regime)) {
            threshold += 0.03;
        } else if ("RANGING".equals(regime)) {
            threshold += 0.01;
        }
        if (pairPerfMult < 0.95) {
            threshold += 0.03;
        }
        return Math.min(threshold, 0.92);
    }

    private static double getAdaptiveRrThreshold(String regime, double pairPerfMult) {
        double threshold = MIN_RISK_REWARD;
        if ("TRENDING".equals(regime)) {
            threshold = 1.8;
        } else if ("VOLATILE".equals(regime)) {
            threshold = 1.7;
        } else if ("ACCUMULATION".equals(regime) || "DISTRIBUTION".equals(regime)) {
            threshold = 1.4;
        } else if ("RANGING".equals(regime)) {
            threshold = 1.6;
        }
        if (pairPerfMult < 0.95) {
            threshold += 0.2;
        }
        return Math.min(threshold, 3.0);
    }

    private static int getRequiredQuantumScore(String regime, double pairPerfMult) {
        int score = 70;
        if ("TRENDING".equals(regime)) {
            score = 70;
        } else if ("VOLATILE".equals(regime)) {
            score = 75;
        } else if ("ACCUMULATION".equals(regime) || "DISTRIBUTION".equals(regime)) {
            score = 68;
        } else if ("RANGING".equals(regime)) {
            score = 72;
        }
        if (pairPerfMult < 0.95) {
            score += 5;
        }
        return Math.min(score, 85);
    }

    private static class TrendStructure {

        final String trend;
        final int structure;

        TrendStructure(String trend, int structure) {
            this.trend = trend;
            this.structure = structure;
        }
    }

    private static TrendStructure getHigherTimeframeTrendStructure(String symbol, String timeframe, int count) {
        java.util.List<Candle> candles = fetchMarketCandles(symbol, count, timeframe);
        if (candles == null || candles.size() < TREND_EMA_LONG + 5) {
            System.out.println("   ⚠️ HTF check failed: not enough " + timeframe + " candles for " + symbol + ".");
            return new TrendStructure("FLAT", 0);
        }

        int lastIndex = candles.size() - 1;
        double currentPrice = candles.get(lastIndex).close;
        double longEma = calculateEMA(candles, lastIndex, TREND_EMA_LONG);
        double shortEma = calculateEMA(candles, lastIndex, TREND_EMA_SHORT);
        int structure = detectMarketStructure(candles, lastIndex, 20);
        boolean emaUp = currentPrice > longEma && shortEma > longEma;
        boolean emaDown = currentPrice < longEma && shortEma < longEma;
        double htfMomentum = (shortEma - longEma) / longEma;
        boolean momentumUp = htfMomentum > (MIN_SIGNAL_MOMENTUM * 0.5); // Relaxed momentum
        boolean momentumDown = htfMomentum < -(MIN_SIGNAL_MOMENTUM * 0.5);

        // Relaxed Trend Logic: Price above Long EMA + (Structure OR Momentum)
        boolean uptrend = (currentPrice > longEma) && (structure == 1 || momentumUp);
        boolean downtrend = (currentPrice < longEma) && (structure == -1 || momentumDown);
        String trend = uptrend ? "UP" : downtrend ? "DOWN" : "FLAT";

        System.out.printf("   📈 HTF %s check for %s: price=%.5f, EMA50=%.5f, EMA200=%.5f, structure=%d, momentum=%.4f -> %s\n",
                timeframe, symbol, currentPrice, shortEma, longEma, structure, htfMomentum, trend);
        return new TrendStructure(trend, structure);
    }
    static final double DEFAULT_RISK_PERCENT = 0.02;
    static final int TREND_EMA_LONG = 200;
    static final int TREND_EMA_SHORT = 50;
    static final int TREND_RSI_PERIOD = 14;
    static final double RSI_BUY_THRESHOLD = 40.0;
    static final double RSI_SELL_THRESHOLD = 60.0;
    static final String LIVE_NEWS_TRADE_MODE_ENV = "LIVE_NEWS_TRADE_MODE";
    static final String LIVE_NEWS_EVENT_ENV = "LIVE_NEWS_EVENT";
    static final String NEWS_CALENDAR_API_URL_ENV = "NEWS_CALENDAR_API_URL";
    static final String NEWS_CALENDAR_API_KEY_ENV = "NEWS_CALENDAR_API_KEY";
    static final String NEWS_PAUSE_WINDOW_MINUTES_ENV = "NEWS_PAUSE_WINDOW_MINUTES";
    static final java.util.List<String> NEWS_FX_CURRENCIES = Arrays.asList("USD", "EUR", "GBP", "JPY", "AUD", "NZD", "CAD", "CHF");
    static final int DEFAULT_NEWS_PAUSE_WINDOW_MINUTES = 15;
    static final String AUTO_CONFIRM_SIGNALS_ENV = "AUTO_CONFIRM_SIGNALS";
    static final String SMTP_HOST_ENV = "SMTP_HOST";
    static final String SMTP_PORT_ENV = "SMTP_PORT";
    static final String SMTP_USERNAME_ENV = "SMTP_USERNAME";
    static final String SMTP_PASSWORD_ENV = "SMTP_PASSWORD";
    static final String SMTP_TLS_ENV = "SMTP_TLS";
    static final String SMTP_STARTTLS_ENV = "SMTP_STARTTLS";
    static final String EMAIL_FROM_ENV = "EMAIL_FROM";
    static final String EMAIL_TO_ENV = "EMAIL_TO";
    static final String DEFAULT_EMAIL_TO = "enohkbosire@gmail.com";
    static final String SEND_EMAIL_NOTIFICATION_ENV = "SEND_EMAIL_NOTIFICATION";
    static final String LIVE_HTF_TIMEFRAME_ENV = "LIVE_HTF_TIMEFRAME";
    static final String LIVE_HTF_CANDLES_COUNT_ENV = "LIVE_HTF_CANDLES_COUNT";
    static final String LIVE_STRATEGY_MODE_ENV = "LIVE_STRATEGY_MODE";
    static final String LIVE_STRATEGY_ARG_PREFIX = "--strategy=";
    static final java.util.List<String> AVAILABLE_LIVE_STRATEGIES = Arrays.asList("smc", "sniper", "meanreversion", "breakout", "auto");
    static final String DEFAULT_LIVE_STRATEGY_MODE = "fusion";
    static final String DISABLE_FOREXBOT_SERVER_ENV = "DISABLE_FOREXBOT_SERVER";
    static final String FOREXBOT_SERVER_PORT_ENV = "FOREXBOT_SERVER_PORT";
    static final String DASHBOARD_PORT_ENV = "DASHBOARD_PORT";
    static final String DISABLE_DASHBOARD_SERVER_ENV = "DISABLE_DASHBOARD_SERVER";
    static final String TRADE_DB_URL_ENV = "TRADE_DB_URL";
    static final String TRADE_DB_USER_ENV = "TRADE_DB_USER";
    static final String TRADE_DB_PASSWORD_ENV = "TRADE_DB_PASSWORD";
    static final String TRADE_DB_DRIVER_ENV = "TRADE_DB_DRIVER";
    static final String TRADE_DB_TABLE_ENV = "TRADE_DB_TABLE";
    static final String MT5_COPIER_ENDPOINTS_ENV = "MT5_COPIER_ENDPOINTS";
    static final String MT5_COPIER_ENABLED_ENV = "MT5_COPIER_ENABLED";
    static final String DEFAULT_DASHBOARD_PORT = "8891";
    static final String DEFAULT_TRADE_DB_TABLE = "trade_history";

    private static double computePreviewScore(TradeSignal signal) {
        double score = signal.mlConfidence * 50 + signal.signalStrength * 30 + signal.smcConfluence * 20;
        score += Math.min(signal.riskRewardRatio, 3.0) * 5;
        return score;
    }

    // ===============================
    // CLASSIFIER INTERFACE
    // ===============================
    interface Classifier {

        int predict(double[] features);

        double predictProbability(double[] features);

        void train(java.util.List<double[]> features, java.util.List<Integer> labels);
    }

    // ===============================
    // NAIVE BAYES CLASSIFIER
    // ===============================
    static class NaiveBayesClassifier implements Classifier, java.io.Serializable {

        double[] meanUp, meanDown;    // means for each feature by class
        double[] varUp, varDown;      // variances for each feature by class
        double probUp, probDown;      // probability of each class
        int numFeatures;

        NaiveBayesClassifier(int numFeatures) {
            this.numFeatures = numFeatures;
            meanUp = new double[numFeatures];
            meanDown = new double[numFeatures];
            varUp = new double[numFeatures];
            varDown = new double[numFeatures];
        }

        public void train(java.util.List<double[]> features, java.util.List<Integer> labels) {
            int countUp = 0, countDown = 0;
            double[][] sumUp = new double[numFeatures][1];
            double[][] sumDown = new double[numFeatures][1];

            // Calculate sums by class
            for (int i = 0; i < labels.size(); i++) {
                if (labels.get(i) == 1) {
                    countUp++;
                    for (int j = 0; j < numFeatures; j++) {
                        sumUp[j][0] += features.get(i)[j];
                    }
                } else {
                    countDown++;
                    for (int j = 0; j < numFeatures; j++) {
                        sumDown[j][0] += features.get(i)[j];
                    }
                }
            }

            // Calculate means
            for (int j = 0; j < numFeatures; j++) {
                meanUp[j] = sumUp[j][0] / countUp;
                meanDown[j] = sumDown[j][0] / countDown;
            }

            // Calculate variances
            for (int i = 0; i < labels.size(); i++) {
                if (labels.get(i) == 1) {
                    for (int j = 0; j < numFeatures; j++) {
                        double diff = features.get(i)[j] - meanUp[j];
                        varUp[j] += diff * diff;
                    }
                } else {
                    for (int j = 0; j < numFeatures; j++) {
                        double diff = features.get(i)[j] - meanDown[j];
                        varDown[j] += diff * diff;
                    }
                }
            }

            for (int j = 0; j < numFeatures; j++) {
                varUp[j] /= countUp;
                varDown[j] /= countDown;

                // Prevent division by zero in Gaussian calculation
                varUp[j] = Math.max(varUp[j], 1e-6);
                varDown[j] = Math.max(varDown[j], 1e-6);
            }

            this.probUp = (double) countUp / labels.size();
            this.probDown = (double) countDown / labels.size();

            // Prevent log(0) in predictions
            this.probUp = Math.max(this.probUp, 1e-6);
            this.probDown = Math.max(this.probDown, 1e-6);
        }

        public int predict(double[] features) {
            double pUp = Math.log(probUp);
            double pDown = Math.log(probDown);

            for (int j = 0; j < numFeatures; j++) {
                double gaussian_up = (1.0 / Math.sqrt(2 * Math.PI * varUp[j]))
                        * Math.exp(-Math.pow(features[j] - meanUp[j], 2) / (2 * varUp[j]));
                double gaussian_down = (1.0 / Math.sqrt(2 * Math.PI * varDown[j]))
                        * Math.exp(-Math.pow(features[j] - meanDown[j], 2) / (2 * varDown[j]));

                pUp += Math.log(gaussian_up + 1e-10);
                pDown += Math.log(gaussian_down + 1e-10);
            }

            return (pUp > pDown) ? 1 : 0;
        }

        public double predictProbability(double[] features) {
            double pUp = Math.log(probUp);
            double pDown = Math.log(probDown);

            for (int j = 0; j < numFeatures; j++) {
                double gaussian_up = (1.0 / Math.sqrt(2 * Math.PI * varUp[j]))
                        * Math.exp(-Math.pow(features[j] - meanUp[j], 2) / (2 * varUp[j]));
                double gaussian_down = (1.0 / Math.sqrt(2 * Math.PI * varDown[j]))
                        * Math.exp(-Math.pow(features[j] - meanDown[j], 2) / (2 * varDown[j]));

                pUp += Math.log(gaussian_up + 1e-10);
                pDown += Math.log(gaussian_down + 1e-10);
            }

            double maxLog = Math.max(pUp, pDown);
            double expUp = Math.exp(pUp - maxLog);
            double expDown = Math.exp(pDown - maxLog);
            double sum = expUp + expDown;
            if (sum == 0 || Double.isNaN(sum)) {
                return 0.5;
            }
            return expUp / sum;
        }

        public void save(String path) throws java.io.IOException {
            try (java.io.ObjectOutputStream oos = new java.io.ObjectOutputStream(new java.io.FileOutputStream(path))) {
                oos.writeObject(this);
            }
        }

        public static NaiveBayesClassifier load(String path) throws java.io.IOException, java.lang.ClassNotFoundException {
            try (java.io.ObjectInputStream ois = new java.io.ObjectInputStream(new java.io.FileInputStream(path))) {
                return (NaiveBayesClassifier) ois.readObject();
            }
        }
    }

    // ===============================
    // RANDOM FOREST CLASSIFIER (Better for correlated features)
    // ===============================
    static class DecisionTree implements java.io.Serializable {

        static class Node implements java.io.Serializable {

            int featureIndex;
            double threshold;
            Node left, right;
            int prediction;

            Node(int featureIndex, double threshold) {
                this.featureIndex = featureIndex;
                this.threshold = threshold;
            }

            Node(int prediction) {
                this.prediction = prediction;
            }
        }

        Node root;

        private static final int MAX_DEPTH = 10;
        private static final int MIN_SAMPLES_SPLIT = 5;
        private java.util.Random rand = new java.util.Random();

        public void train(java.util.List<double[]> features, java.util.List<Integer> labels,
                java.util.List<Integer> featureIndices) {
            root = buildTree(features, labels, featureIndices, 0);
        }

        private Node buildTree(java.util.List<double[]> features, java.util.List<Integer> labels,
                java.util.List<Integer> featureIndices, int depth) {
            if (depth >= MAX_DEPTH || labels.size() < MIN_SAMPLES_SPLIT) {
                return new Node(majorityClass(labels));
            }

            // Find best split with random feature selection at every node
            double bestGini = Double.MAX_VALUE;
            int bestFeature = -1;
            double bestThreshold = 0;

            int subsetSize = Math.max(1, (int) Math.sqrt(featureIndices.size()));
            java.util.List<Integer> subset = new ArrayList<>();
            while (subset.size() < subsetSize) {
                int candidate = featureIndices.get(rand.nextInt(featureIndices.size()));
                if (!subset.contains(candidate)) {
                    subset.add(candidate);
                }
            }

            for (int f : subset) {
                java.util.List<Double> values = new ArrayList<>();
                for (double[] feature : features) {
                    values.add(feature[f]);
                }
                java.util.Collections.sort(values);

                for (int i = 1; i < values.size(); i++) {
                    double threshold = (values.get(i - 1) + values.get(i)) / 2;
                    double gini = calculateGini(features, labels, f, threshold);
                    if (gini < bestGini) {
                        bestGini = gini;
                        bestFeature = f;
                        bestThreshold = threshold;
                    }
                }
            }

            if (bestFeature == -1) {
                return new Node(majorityClass(labels));
            }

            // Split data
            java.util.List<double[]> leftFeatures = new ArrayList<>();
            java.util.List<double[]> rightFeatures = new ArrayList<>();
            java.util.List<Integer> leftLabels = new ArrayList<>();
            java.util.List<Integer> rightLabels = new ArrayList<>();

            for (int i = 0; i < features.size(); i++) {
                if (features.get(i)[bestFeature] <= bestThreshold) {
                    leftFeatures.add(features.get(i));
                    leftLabels.add(labels.get(i));
                } else {
                    rightFeatures.add(features.get(i));
                    rightLabels.add(labels.get(i));
                }
            }

            if (leftFeatures.isEmpty() || rightFeatures.isEmpty()) {
                return new Node(majorityClass(labels));
            }

            Node node = new Node(bestFeature, bestThreshold);
            node.left = buildTree(leftFeatures, leftLabels, featureIndices, depth + 1);
            node.right = buildTree(rightFeatures, rightLabels, featureIndices, depth + 1);
            return node;
        }

        private int majorityClass(java.util.List<Integer> labels) {
            int count0 = 0, count1 = 0;
            for (int label : labels) {
                if (label == 0) {
                    count0++;
                } else {
                    count1++;
                }
            }
            return count1 > count0 ? 1 : 0;

        }

        public void collectFeatureUsage(int[] counts) {
            collectFeatureUsage(root, counts);
        }

        private void collectFeatureUsage(Node node, int[] counts) {
            if (node == null || node.left == null || node.right == null) {
                return;
            }
            if (node.featureIndex >= 0 && node.featureIndex < counts.length) {
                counts[node.featureIndex]++;
            }
            collectFeatureUsage(node.left, counts);
            collectFeatureUsage(node.right, counts);
        }

        private double calculateGini(java.util.List<double[]> features, java.util.List<Integer> labels,
                int feature, double threshold) {
            int left0 = 0, left1 = 0, right0 = 0, right1 = 0;

            for (int i = 0; i < features.size(); i++) {
                if (features.get(i)[feature] <= threshold) {
                    if (labels.get(i) == 0) {
                        left0++;
                    } else {
                        left1++;
                    }
                } else {
                    if (labels.get(i) == 0) {
                        right0++;
                    } else {
                        right1++;
                    }
                }
            }

            double leftTotal = left0 + left1;
            double rightTotal = right0 + right1;
            double total = leftTotal + rightTotal;

            double leftGini = leftTotal > 0 ? 1 - Math.pow(left0 / leftTotal, 2) - Math.pow(left1 / leftTotal, 2) : 0;
            double rightGini = rightTotal > 0 ? 1 - Math.pow(right0 / rightTotal, 2) - Math.pow(right1 / rightTotal, 2) : 0;

            return (leftTotal / total) * leftGini + (rightTotal / total) * rightGini;
        }

        public int predict(double[] features) {
            Node node = root;
            while (node.left != null) {
                if (features[node.featureIndex] <= node.threshold) {
                    node = node.left;
                } else {
                    node = node.right;
                }
            }
            return node.prediction;
        }
    }

    static class RandomForestClassifier implements Classifier, java.io.Serializable {

        java.util.List<DecisionTree> trees;
        int numTrees;
        int numFeatures;
        double[] featureImportances;

        RandomForestClassifier(int numTrees, int numFeatures) {
            this.numTrees = numTrees;
            this.numFeatures = numFeatures;
            trees = new ArrayList<>();
        }

        public void train(java.util.List<double[]> features, java.util.List<Integer> labels) {
            trees = new ArrayList<>();
            java.util.Random rand = new java.util.Random();

            java.util.List<Integer> order = new ArrayList<>();
            for (int i = 0; i < features.size(); i++) {
                order.add(i);
            }
            java.util.Collections.shuffle(order, rand);

            java.util.List<double[]> shuffledFeatures = new ArrayList<>();
            java.util.List<Integer> shuffledLabels = new ArrayList<>();
            for (int idx : order) {
                shuffledFeatures.add(features.get(idx));
                shuffledLabels.add(labels.get(idx));
            }

            // Balance classes by bootstrapped resampling to reduce bias
            java.util.List<Integer> class0 = new ArrayList<>();
            java.util.List<Integer> class1 = new ArrayList<>();
            for (int i = 0; i < shuffledLabels.size(); i++) {
                if (shuffledLabels.get(i) == 0) {
                    class0.add(i);
                } else {
                    class1.add(i);
                }
            }
            int targetSize = Math.max(class0.size(), class1.size());
            if (targetSize == 0) {
                targetSize = shuffledLabels.size();
            }
            if (class0.isEmpty() || class1.isEmpty()) {
                targetSize = shuffledLabels.size();
            }

            for (int t = 0; t < numTrees; t++) {
                java.util.List<double[]> sampleFeatures = new ArrayList<>();
                java.util.List<Integer> sampleLabels = new ArrayList<>();

                if (class0.isEmpty() || class1.isEmpty()) {
                    for (int i = 0; i < shuffledFeatures.size(); i++) {
                        int idx = rand.nextInt(shuffledFeatures.size());
                        sampleFeatures.add(shuffledFeatures.get(idx));
                        sampleLabels.add(shuffledLabels.get(idx));
                    }
                } else {
                    for (int i = 0; i < targetSize; i++) {
                        int idx0 = class0.get(rand.nextInt(class0.size()));
                        sampleFeatures.add(shuffledFeatures.get(idx0));
                        sampleLabels.add(shuffledLabels.get(idx0));

                        int idx1 = class1.get(rand.nextInt(class1.size()));
                        sampleFeatures.add(shuffledFeatures.get(idx1));
                        sampleLabels.add(shuffledLabels.get(idx1));
                    }
                }

                // Random feature subset
                java.util.List<Integer> featureIndices = new ArrayList<>();
                int subsetSize = Math.max(1, (int) Math.sqrt(numFeatures));
                while (featureIndices.size() < subsetSize) {
                    int f = rand.nextInt(numFeatures);
                    if (!featureIndices.contains(f)) {
                        featureIndices.add(f);
                    }
                }

                DecisionTree tree = new DecisionTree();
                tree.train(sampleFeatures, sampleLabels, featureIndices);
                trees.add(tree);
            }
        }

        public int predict(double[] features) {
            int[] votes = new int[2];
            for (DecisionTree tree : trees) {
                int pred = tree.predict(features);
                votes[pred]++;
            }
            return votes[1] > votes[0] ? 1 : 0;
        }

        public double predictProbability(double[] features) {
            int[] votes = new int[2];
            for (DecisionTree tree : trees) {
                int pred = tree.predict(features);
                votes[pred]++;
            }
            return (double) votes[1] / numTrees;
        }

        public void computeFeatureImportances() {
            featureImportances = new double[numFeatures];
            int[] counts = new int[numFeatures];
            for (DecisionTree tree : trees) {
                tree.collectFeatureUsage(counts);
            }
            double total = 0;
            for (int count : counts) {
                total += count;
            }
            if (total == 0) {
                total = 1;
            }
            for (int i = 0; i < numFeatures; i++) {
                featureImportances[i] = counts[i] / total;
            }
        }

        public double[] getFeatureImportances() {
            if (featureImportances == null) {
                computeFeatureImportances();
            }
            return featureImportances;
        }

        public void save(String path) throws java.io.IOException {
            try (java.io.ObjectOutputStream oos = new java.io.ObjectOutputStream(new java.io.FileOutputStream(path))) {
                oos.writeObject(this);
            }
        }

        public static RandomForestClassifier load(String path) throws java.io.IOException, java.lang.ClassNotFoundException {
            try (java.io.ObjectInputStream ois = new java.io.ObjectInputStream(new java.io.FileInputStream(path))) {
                return (RandomForestClassifier) ois.readObject();
            }
        }
    }

    static class FeatureScaler implements java.io.Serializable {

        private double[] mean;
        private double[] std;

        FeatureScaler(int numFeatures) {
            mean = new double[numFeatures];
            std = new double[numFeatures];
        }

        public java.util.List<double[]> fitTransform(java.util.List<double[]> features) {
            int n = features.size();
            int m = mean.length;

            Arrays.fill(mean, 0);
            Arrays.fill(std, 0);

            for (double[] feature : features) {
                for (int j = 0; j < m; j++) {
                    mean[j] += feature[j];
                }
            }
            for (int j = 0; j < m; j++) {
                mean[j] /= Math.max(1, n);
            }

            for (double[] feature : features) {
                for (int j = 0; j < m; j++) {
                    double diff = feature[j] - mean[j];
                    std[j] += diff * diff;
                }
            }
            for (int j = 0; j < m; j++) {
                std[j] = Math.sqrt(std[j] / Math.max(1, n));
                std[j] = Math.max(std[j], 1e-6);
            }

            java.util.List<double[]> transformed = new ArrayList<>();
            for (double[] feature : features) {
                transformed.add(transform(feature));
            }
            return transformed;
        }

        public double[] transform(double[] feature) {
            double[] scaled = new double[mean.length];
            for (int j = 0; j < mean.length; j++) {
                scaled[j] = (feature[j] - mean[j]) / std[j];
            }
            return scaled;
        }

        public void save(String path) throws java.io.IOException {
            try (java.io.ObjectOutputStream oos = new java.io.ObjectOutputStream(new java.io.FileOutputStream(path))) {
                oos.writeObject(this);
            }
        }

        public static FeatureScaler load(String path) throws java.io.IOException, java.lang.ClassNotFoundException {
            try (java.io.ObjectInputStream ois = new java.io.ObjectInputStream(new java.io.FileInputStream(path))) {
                return (FeatureScaler) ois.readObject();
            }
        }
    }

    private static boolean containsArg(String[] args, String name) {
        if (args == null || args.length == 0) {
            return false;
        }
        for (String arg : args) {
            if (arg != null && arg.equalsIgnoreCase(name)) {
                return true;
            }
        }
        return false;
    }

    private static String getArgValue(String[] args, String prefix) {
        if (args == null || args.length == 0 || prefix == null) {
            return null;
        }
        for (String arg : args) {
            if (arg != null && arg.toLowerCase().startsWith(prefix.toLowerCase())) {
                return arg.substring(prefix.length()).trim();
            }
        }
        return null;
    }

    private static String firstNonEmpty(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isEmpty()) {
                return value;
            }
        }
        return null;
    }

    private static String normalizeLiveStrategyMode(String strategy) {
        if (strategy == null) {
            return DEFAULT_LIVE_STRATEGY_MODE;
        }
        strategy = strategy.trim().toLowerCase(Locale.ROOT);
        switch (strategy) {
            case "meanreversion":
            case "mean-reversion":
            case "mean":
                return "meanreversion";
            case "breakout":
            case "break":
                return "breakout";
            case "smc":
                return "smc";
            case "all":
                return "all";
            case "trend":
            case "trendpullback":
            case "pullback":
                return "smc";
            case "sniper":
            case "sniper-mode":
                return "sniper";
            default:
                return strategy;
        }
    }

    private static boolean isValidLiveStrategyMode(String strategy) {
        return "trend".equals(strategy) || "sniper".equals(strategy) || "meanreversion".equals(strategy) || "breakout".equals(strategy) || "smc".equals(strategy) || "auto".equals(strategy) || "all".equals(strategy);
    }

    public static void main(String[] args) throws Exception {

        // Start the Cloud Spark API and Dashboard
        new Thread(() -> {
            try {
                System.out.println("🚀 Initializing Cloud API...");
                CloudAPI.start();
            } catch (Exception e) {
                System.err.println("❌ Cloud API Error: " + e.getMessage());
            }
        }).start();

        boolean liveMode = containsArg(args, "live");
        boolean serverMode = containsArg(args, "server");
        boolean chartMode = containsArg(args, "chart");
        boolean testSignalMode = containsArg(args, "testsignal");
        boolean debugMode = containsArg(args, "debug");

        if (!liveMode && !serverMode && !chartMode && !testSignalMode) {
            liveMode = true;
            System.out.println("▶ No explicit mode provided; defaulting to live candle market mode.");
        }

        MT5_BASE_ENDPOINT_OVERRIDE = getArgValue(args, "--mt5-base-endpoint=");
        MT5_ENDPOINT_OVERRIDE = getArgValue(args, "--mt5-endpoint=");
        MT5_CHART_ENDPOINT_OVERRIDE = getArgValue(args, "--mt5-chart-endpoint=");
        MT5_SYMBOLS_ENDPOINT_OVERRIDE = getArgValue(args, "--mt5-symbols-endpoint=");

        tradeDatabase.initialize();
        printMt5EndpointInfo();

        if (serverMode) {
            System.out.println("🚀 Starting MT5 signal receiver server only...");
            startDashboardServer();
            startForexBotServer();
            return;
        }

        if (testSignalMode) {
            System.out.println("🚀 TEST SIGNAL MODE ENABLED");
            TradeSignal testSignal = new TradeSignal("EURUSD", "BUY", 1.1765, 1.1755, 1.1785,
                    0.75, 0.85, 80.0, "Forced test signal", 10.0, 30.0);
            MobileSignalBridge.sendToMobile(testSignal.symbol, testSignal.direction, testSignal.entry,
                    testSignal.takeProfit, testSignal.stopLoss, testSignal.mlConfidence,
                    testSignal.signalStrength, testSignal.reason, testSignal.riskRewardRatio, "TEST", "CORE");
            sendSignalToMT5(testSignal);
            return;
        }

        if (chartMode) {
            String symbol = DEFAULT_CHART_SYMBOL;
            String timeframe = "M5";
            int count = 80;
            if (args.length > 1) {
                symbol = args[1].trim().toUpperCase();
            }
            if (args.length > 2) {
                timeframe = args[2].trim().toUpperCase();
            }
            if (args.length > 3) try {
                count = Integer.parseInt(args[3]);
            } catch (Exception ignored) {
            }
            showLiveMarketChart(symbol, count, timeframe, 10);
            return;
        }

        if (liveMode) {
            System.out.println("🚀 [MODE] QUANTUM FUSION ENGINE (QFE) ACTIVE");
            
            // Start servers in a background thread to prevent blocking the scanner
            new Thread(() -> {
                try {
                    startDashboardServer();
                    startForexBotServer();
                } catch (Exception e) {
                    System.err.println("❌ Server Error: " + e.getMessage());
                }
            }).start();

            int liveCount = parseIntEnv("LIVE_CANDLES_COUNT", 220);
            String liveTimeframe = System.getenv().getOrDefault("LIVE_TIMEFRAME", "M5");

            while (true) {
                cloudCache.clear();
                boolean forexClosed = isForexMarketClosed();
                if (forexClosed) {
                    ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
                    System.out.printf("😴 Forex Market Closed (UTC: %s). Checking for crypto opportunities...%n",
                            now.format(java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME));
                }

                java.util.List<String> liveSymbols = getLiveSymbols(args);
                for (String symbol : liveSymbols) {
                    if (forexClosed && !Fxausd.isCrypto(symbol)) {
                        continue;
                    }

                    System.out.println("\n🔍 [Audit] Scanning " + symbol + "...");
                    CloudAPI.updateBotStatus("SCANNING", "Quantum QILH Scan: " + symbol);
                    java.util.List<Candle> symbolCandles = fetchMarketCandles(symbol, liveCount, liveTimeframe);
                    if (symbolCandles == null || symbolCandles.isEmpty()) {
                        continue;
                    }

                    updateGlobalIntelligence(symbol, symbolCandles);
                    
                    // --- THE QUANTUM FUSION ENGINE (QFE) ---
                    // Using generateQuantumFusionSignals as the primary institutional decision core
                    java.util.List<TradeSignal> fusionSignals = generateQuantumFusionSignals(symbolCandles, symbol, liveTimeframe);

                    if (!fusionSignals.isEmpty()) {
                        System.out.println("🔥 [QFE EXECUTE] Institutional Fusion Consensus Reached for " + symbol);
                        sendLiveSignals(fusionSignals);
                    } else {
                        // System.out.println("⏳ [Idle] " + symbol + ": Monitoring for A+ Fusion confluence...");
                    }

                    
                    // --- RATE LIMIT PROTECTION ---
                    // Free Twelve Data only allows 8 requests/min. 
                    // Sleep 10s between symbols to stay under the limit.
                    Thread.sleep(10000);
                }
            
                // --- CYCLE SLEEP ---
                // Wait exactly 3 minutes after all symbols have been scanned
                int sleepMins = 3;
                System.out.println("💤 Cycle complete. Sleeping for " + sleepMins + " minutes...");
                Thread.sleep(60000 * sleepMins);
            }
        }
    }

    // ===============================
    // LIVE MT5 SIGNAL DISPATCH
    // ===============================
    private static String normalizeEndpoint(String endpoint) {
        return endpoint == null ? null : endpoint.trim();
    }

    public static final String MT5_BASE_ENDPOINT = normalizeEndpoint(System.getenv("MT5_BASE_ENDPOINT"));
    public static final String MT5_ENDPOINT = normalizeEndpoint(System.getenv("MT5_ENDPOINT"));
    public static final String MT5_API_KEY = normalizeEndpoint(System.getenv("MT5_API_KEY"));
    public static String MT5_BASE_ENDPOINT_OVERRIDE = null;
    public static String MT5_ENDPOINT_OVERRIDE = null;
    public static String MT5_CHART_ENDPOINT_OVERRIDE = null;
    public static String MT5_SYMBOLS_ENDPOINT_OVERRIDE = null;
    public static final String DEFAULT_MT5_BASE_ENDPOINT = "http://127.0.0.1:5005/api";
    public static final String DEFAULT_MT5_LEGACY_BASE_ENDPOINT = "https://fxausd.onrender.com/api";
    public static final String DEFAULT_MT5_FALLBACK_BASE_ENDPOINT = "http://127.0.0.1:5000/api";
    public static final String DEFAULT_MT5_FALLBACK_BASE_ENDPOINT_2 = "https://fxausd-bridge.onrender.com/api";
    public static final String DEFAULT_MT5_FALLBACK_BASE_ENDPOINT_3 = "http://127.0.0.1:5001/api";
    public static final String DEFAULT_MT5_ENDPOINT = DEFAULT_MT5_BASE_ENDPOINT + "/order";
    public static final String DEFAULT_MT5_FALLBACK_ENDPOINT = DEFAULT_MT5_FALLBACK_BASE_ENDPOINT + "/order";
    // Use a LinkedHashMap limited to the last 500 entries to prevent memory leak
    private static final java.util.Map<String, Long> sentSignalKeys = java.util.Collections.synchronizedMap(
            new java.util.LinkedHashMap<String, Long>(512, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(java.util.Map.Entry<String, Long> eldest) {
            return size() > 500 || (System.currentTimeMillis() - eldest.getValue()) > 3_600_000L; // 1 hr TTL
        }
    });
    public static final java.util.List<TradeSignal> recentLiveSignals = Collections.synchronizedList(new ArrayList<>());
    public static final java.util.List<TradeRecord> recentTradeRecords = Collections.synchronizedList(new ArrayList<>());
    public static final TradeDatabase tradeDatabase = new TradeDatabase();
    private static HttpServer dashboardServer = null;

    private static String escapeJson(String input) {
        return input.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
    }

    public static final String JUSTMARKETS_SUFFIX = System.getenv().getOrDefault("MT5_SYMBOL_SUFFIX", ""); // e.g. ".pro" or ".m"

    public static String buildMt5Payload(TradeSignal signal) {
        String symbol = (signal.symbol != null ? signal.symbol : "XAUUSD") + JUSTMARKETS_SUFFIX;
        return String.format(
                "{\"symbol\":\"%s\",\"direction\":\"%s\",\"entry\":%.5f,\"stopLoss\":%.5f,\"takeProfit\":%.5f,\"confidence\":%.4f,\"signalStrength\":%.4f,\"volume\":%.2f,\"reason\":\"%s\"}",
                escapeJson(symbol), escapeJson(signal.direction), signal.entry,
                signal.stopLoss, signal.takeProfit, signal.mlConfidence,
                signal.signalStrength, signal.lotSize, escapeJson(signal.reason)
        );
    }

    private static String formatMt5ResponseDetails(String response) {
        if (response == null || response.isEmpty()) {
            return response;
        }

        try {
            java.util.Map<String, Object> map = new Gson().fromJson(response, new TypeToken<java.util.Map<String, Object>>() {
            }.getType());
            java.util.List<String> details = new ArrayList<>();
            if (map.containsKey("orderId")) {
                details.add("orderId=" + map.get("orderId"));
            }
            if (map.containsKey("ticket")) {
                details.add("ticket=" + map.get("ticket"));
            }
            if (map.containsKey("status")) {
                details.add("status=" + map.get("status"));
            }
            if (map.containsKey("message")) {
                details.add("message=" + map.get("message"));
            }
            if (map.containsKey("error")) {
                details.add("error=" + map.get("error"));
            }
            if (!details.isEmpty()) {
                return String.join(", ", details);
            }
        } catch (Exception ignored) {
        }

        return response;
    }

    public static boolean sendSignalToMT5(TradeSignal signal) {
        if (ForexBot.isForexMarketClosed()) {
            System.out.println("❌ Forex market is closed. Skipping MT5 dispatch.");
            return false;
        }

        java.util.List<String> endpoints = getMt5OrderEndpoints();
        if (endpoints.isEmpty()) {
            System.out.println("❌ No MT5 order endpoint configured. Set MT5_ENDPOINT or MT5_BASE_ENDPOINT.");
            return false;
        }

        System.out.println("▶ MT5 endpoints to try: " + endpoints);
        boolean anySuccess = false;
        for (String endpoint : endpoints) {
            if (dispatchSignalToMt5Endpoint(signal, endpoint)) {
                anySuccess = true;
                break;
            }
        }

        return anySuccess;
    }

    private static java.util.List<String> buildMt5BaseCandidates(String baseEndpoint) {
        java.util.LinkedHashSet<String> candidates = new java.util.LinkedHashSet<>();
        String normalized = normalizeEndpoint(baseEndpoint);
        if (normalized == null || normalized.isEmpty()) {
            return new ArrayList<>();
        }
        normalized = normalized.replaceAll("/+$", "");
        candidates.add(normalized);
        if (normalized.toLowerCase().endsWith("/api")) {
            String withoutApi = normalized.substring(0, normalized.length() - 4);
            if (!withoutApi.isEmpty()) {
                candidates.add(withoutApi);
            }
        } else {
            candidates.add(normalized + "/api");
        }
        return new ArrayList<>(candidates);
    }

    private static String normalizeMt5OrderEndpoint(String endpoint) {
        String normalized = normalizeEndpoint(endpoint);
        if (normalized == null || normalized.isEmpty()) {
            return null;
        }
        normalized = normalized.replaceAll("/+$", "");
        String lower = normalized.toLowerCase();
        if (lower.endsWith("/order") || lower.endsWith("/api/order")) {
            return normalized;
        }
        if (lower.endsWith("/api")) {
            return normalized + "/order";
        }
        return normalized + "/order";
    }

    private static java.util.List<String> getMt5OrderEndpoints() {
        java.util.LinkedHashSet<String> endpoints = new java.util.LinkedHashSet<>();

        // Priority 1: Command line override
        String primary = firstNonEmpty(MT5_ENDPOINT_OVERRIDE, MT5_ENDPOINT);
        if (primary != null && !primary.trim().isEmpty()) {
            endpoints.add(normalizeMt5OrderEndpoint(primary));
        }

        // Priority 2: The known active local bridge (Port 5005)
        endpoints.add("http://127.0.0.1:5005/api/order");
        endpoints.add("http://127.0.0.1:5005/order");

        // Priority 3: Other fallbacks
        java.util.List<String> baseCandidates = buildMt5BaseCandidates(firstNonEmpty(MT5_BASE_ENDPOINT_OVERRIDE, MT5_BASE_ENDPOINT));
        for (String base : baseCandidates) {
            endpoints.add(normalizeMt5OrderEndpoint(base));
        }

        endpoints.add(normalizeMt5OrderEndpoint(DEFAULT_MT5_BASE_ENDPOINT));
        endpoints.add(normalizeMt5OrderEndpoint(DEFAULT_MT5_FALLBACK_BASE_ENDPOINT)); // 5000
        endpoints.add(normalizeMt5OrderEndpoint(DEFAULT_MT5_FALLBACK_BASE_ENDPOINT_3)); // 5001

        String copierEndpoints = System.getenv(MT5_COPIER_ENDPOINTS_ENV);
        if (copierEndpoints != null && !copierEndpoints.trim().isEmpty()) {
            for (String raw : copierEndpoints.split(",")) {
                String normalized = normalizeMt5OrderEndpoint(raw);
                if (normalized != null && !normalized.isEmpty()) {
                    endpoints.add(normalized);
                }
            }
        }

        return new ArrayList<>(endpoints);
    }

    private static String getMt5ChartEndpoint() {
        return getMt5ChartEndpoints().get(0);
    }

    private static java.util.List<String> getMt5ChartEndpoints() {
        String endpoint = firstNonEmpty(MT5_CHART_ENDPOINT_OVERRIDE, MT5_CHART_ENDPOINT);
        if (endpoint != null && !endpoint.trim().isEmpty()) {
            return Collections.singletonList(endpoint.trim());
        }

        java.util.LinkedHashSet<String> endpoints = new java.util.LinkedHashSet<>();
        java.util.List<String> baseCandidates = buildMt5BaseCandidates(firstNonEmpty(MT5_BASE_ENDPOINT_OVERRIDE, MT5_BASE_ENDPOINT));
        for (String base : baseCandidates) {
            endpoints.add(base + "/candles");
        }
        for (String base : buildMt5BaseCandidates(DEFAULT_MT5_BASE_ENDPOINT)) {
            endpoints.add(base + "/candles");
        }
        for (String base : buildMt5BaseCandidates(DEFAULT_MT5_LEGACY_BASE_ENDPOINT)) {
            endpoints.add(base + "/candles");
        }
        for (String base : buildMt5BaseCandidates(DEFAULT_MT5_FALLBACK_BASE_ENDPOINT)) {
            endpoints.add(base + "/candles");
        }
        for (String base : buildMt5BaseCandidates(DEFAULT_MT5_FALLBACK_BASE_ENDPOINT_2)) {
            endpoints.add(base + "/candles");
        }
        for (String base : buildMt5BaseCandidates(DEFAULT_MT5_FALLBACK_BASE_ENDPOINT_3)) {
            endpoints.add(base + "/candles");
        }
        return new ArrayList<>(endpoints);
    }

    private static String getMt5SymbolsEndpoint() {
        return getMt5SymbolsEndpoints().get(0);
    }

    private static java.util.List<String> getMt5SymbolsEndpoints() {
        String endpoint = firstNonEmpty(MT5_SYMBOLS_ENDPOINT_OVERRIDE, MT5_SYMBOLS_ENDPOINT);
        if (endpoint != null && !endpoint.trim().isEmpty()) {
            return Collections.singletonList(endpoint.trim());
        }

        java.util.LinkedHashSet<String> endpoints = new java.util.LinkedHashSet<>();
        java.util.List<String> baseCandidates = buildMt5BaseCandidates(firstNonEmpty(MT5_BASE_ENDPOINT_OVERRIDE, MT5_BASE_ENDPOINT));
        for (String base : baseCandidates) {
            endpoints.add(base + "/symbols");
        }
        String chartEndpoint = firstNonEmpty(MT5_CHART_ENDPOINT_OVERRIDE, MT5_CHART_ENDPOINT);
        if (chartEndpoint != null && !chartEndpoint.trim().isEmpty()) {
            String normalizedChart = chartEndpoint.replaceAll("/+$", "");
            if (normalizedChart.toLowerCase().endsWith("/candles")) {
                endpoints.add(normalizedChart.substring(0, normalizedChart.length() - 8) + "/symbols");
            } else {
                endpoints.add(normalizedChart + "/symbols");
            }
        }
        for (String base : buildMt5BaseCandidates(DEFAULT_MT5_BASE_ENDPOINT)) {
            endpoints.add(base + "/symbols");
        }
        for (String base : buildMt5BaseCandidates(DEFAULT_MT5_LEGACY_BASE_ENDPOINT)) {
            endpoints.add(base + "/symbols");
        }
        for (String base : buildMt5BaseCandidates(DEFAULT_MT5_FALLBACK_BASE_ENDPOINT)) {
            endpoints.add(base + "/symbols");
        }
        for (String base : buildMt5BaseCandidates(DEFAULT_MT5_FALLBACK_BASE_ENDPOINT_2)) {
            endpoints.add(base + "/symbols");
        }
        for (String base : buildMt5BaseCandidates(DEFAULT_MT5_FALLBACK_BASE_ENDPOINT_3)) {
            endpoints.add(base + "/symbols");
        }
        return new ArrayList<>(endpoints);
    }

    private static void printMt5EndpointInfo() {
        System.out.println("▶ MT5 endpoint configuration:");
        System.out.println("   Order candidates: " + String.join(", ", getMt5OrderEndpoints()));
        System.out.println("   Chart candidates: " + String.join(", ", getMt5ChartEndpoints()));
        System.out.println("   Symbol candidates: " + String.join(", ", getMt5SymbolsEndpoints()));
        if (MT5_API_KEY != null && !MT5_API_KEY.isEmpty()) {
            System.out.println("   Using MT5_API_KEY authentication.");
        }
    }

    private static void printUsage() {
        System.out.println("Fxausd usage:");
        System.out.println("  java -jar Fxausd.jar [live] [server] [chart] [testsignal] [crt] [combo] [debug] [options]");
        System.out.println("Modes:");
        System.out.println("  live          Default when no explicit mode is supplied.");
        System.out.println("  server        Start the dashboard/server mode only.");
        System.out.println("  chart         Display market chart data.");
        System.out.println("  testsignal    Run signal generation without live execution.");
        System.out.println("Endpoint override options:");
        System.out.println("  --mt5-base-endpoint=<url>    Override the MT5 bridge base endpoint.");
        System.out.println("  --mt5-endpoint=<url>         Override the MT5 order endpoint.");
        System.out.println("  --mt5-chart-endpoint=<url>   Override the MT5 chart/candles endpoint.");
        System.out.println("  --mt5-symbols-endpoint=<url> Override the MT5 symbols endpoint.");
        System.out.println("  --strategy=<smc|sniper|meanreversion|breakout|all>    Select live signal strategy.");
        System.out.println("     'all' runs each strategy independently and reports separate signals.");
        System.out.println("     sniper entry is only applied to XAUUSD; all other symbols use smc.");
        System.out.println("  --min-signal-momentum=<decimal>   Lower or raise the momentum cutoff for live signals.");
        System.out.println("  live ALL      Run live mode on all available MT5 symbols.");
        System.out.println("Environment variables:");
        System.out.println("  LIVE_SYMBOLS  Comma/space-separated live symbols, or ALL to fetch available MT5 symbols.");
        System.out.println("  LIVE_SIGNAL_MOMENTUM  Decimal threshold for live momentum filtering.");
        System.out.println("  MT5_BASE_ENDPOINT, MT5_ENDPOINT, MT5_CHART_ENDPOINT, MT5_SYMBOLS_ENDPOINT, MT5_API_KEY");
        System.out.println("  LIVE_STRATEGY_MODE, LIVE_TIMEFRAME, LIVE_CANDLES_COUNT, LIVE_HTF_TIMEFRAME, LIVE_HTF_CANDLES_COUNT");
        System.out.println("Example:");
        System.out.println("  java -jar Fxausd.jar live --mt5-base-endpoint=https://fxausd.onrender.com/api");
    }

    private static boolean dispatchSignalToMt5Endpoint(TradeSignal signal, String endpoint) {
        String normalizedEndpoint = endpoint.trim();
        String signalKey = normalizedEndpoint + "_" + signal.symbol + "_" + signal.direction + "_" + String.format("%.5f", signal.entry);
        if (sentSignalKeys.containsKey(signalKey)) {
            System.out.println("⚠️ Duplicate MT5 signal blocked for endpoint " + normalizedEndpoint + ": " + signalKey);
            return false;
        }

        int maxAttempts = 3;
        int timeoutMs = 5000;
        String payload = buildMt5Payload(signal);

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                URL url = new URL(normalizedEndpoint);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(timeoutMs);
                conn.setReadTimeout(timeoutMs);
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                if (MT5_API_KEY != null && !MT5_API_KEY.isEmpty()) {
                    conn.setRequestProperty("Authorization", "Bearer " + MT5_API_KEY);
                }
                conn.setDoOutput(true);

                byte[] bytes = payload.getBytes("UTF-8");
                conn.setFixedLengthStreamingMode(bytes.length);
                conn.getOutputStream().write(bytes);

                int status = conn.getResponseCode();
                String response = "";
                java.io.InputStream responseStream = null;
                try {
                    if (status >= 200 && status < 300) {
                        responseStream = conn.getInputStream();
                    } else {
                        responseStream = conn.getErrorStream();
                    }
                    if (responseStream == null) {
                        responseStream = conn.getInputStream();
                    }
                    try (java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(responseStream))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            response += line;
                        }
                    }
                } catch (Exception ignored) {
                }
                conn.disconnect();

                if (status >= 200 && status < 300) {
                    sentSignalKeys.put(signalKey, System.currentTimeMillis());
                    System.out.println("✅ Sent MT5 order to " + normalizedEndpoint + ": " + signal.direction + " " + String.format("%.5f", signal.entry));
                    if (!response.isEmpty()) {
                        System.out.println("   MT5 response: " + formatMt5ResponseDetails(response));
                    }
                    return true;
                }

                System.out.println("⚠️ MT5 dispatch returned " + status + " from " + normalizedEndpoint + ": " + formatMt5ResponseDetails(response));
                System.out.println("   Payload: " + payload);
            } catch (Exception e) {
                System.out.println("❌ MT5 dispatch attempt " + attempt + " to " + normalizedEndpoint + " failed: " + e.getMessage());
            }

            if (attempt < maxAttempts) {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException ignored) {
                }
            }
        }

        return false;
    }

    public static void sendLiveSignals(java.util.List<TradeSignal> signals) {
        if (signals == null || signals.isEmpty()) {
            // Log silence during continuous run
            return;
        }

        if (ForexBot.isForexMarketClosed()) {
            return;
        }

        for (TradeSignal signal : signals) {
            // Professional Logging
            System.out.println("🚀 [AI BOT] DISPATCHING SIGNAL: " + signal.symbol + " " + signal.direction);

            // Push to Mobile App (Cloud Ready)
            MobileSignalBridge.sendToMobile(
                    signal.symbol,
                    signal.direction,
                    signal.entry,
                    signal.takeProfit,
                    signal.stopLoss,
                    signal.mlConfidence,
                    signal.signalStrength,
                    signal.reason,
                    signal.riskRewardRatio,
                    signal.session,
                    signal.setupType
            );

            // Push to MT5 (Execution)
            boolean ok = sendSignalToMT5(signal);

            // Copy Trading Simulation
            if (ok) {
                System.out.println("🔗 [COPY TRADING] Mirroring trade for followers...");
            }
        }
    }

    static void recordTrade(TradeRecord record) {
        if (record == null) {
            return;
        }
        recentTradeRecords.add(record);
        if (tradeDatabase != null && tradeDatabase.isEnabled()) {
            tradeDatabase.insertTrade(record);
        }
    }

    public static boolean confirmLiveExecution(java.util.List<TradeSignal> signals) {
        String auto = System.getenv(AUTO_CONFIRM_SIGNALS_ENV);
        if (auto != null && (auto.equalsIgnoreCase("1") || auto.equalsIgnoreCase("true") || auto.equalsIgnoreCase("yes"))) {
            System.out.println("✅ AUTO_CONFIRM_SIGNALS enabled. Live execution confirmed automatically.");
            return true;
        }

        System.out.println("\n🔔 LIVE TRADE CONFIRMATION REQUIRED");
        System.out.println("   Signals ready for execution: " + signals.size());
        for (int i = 0; i < signals.size(); i++) {
            TradeSignal s = signals.get(i);
            System.out.printf("   [%d] %s %s Entry=%.5f SL=%.5f TP=%.5f ML=%.1f%% SMC=%.1f%% Strength=%.1f%% R:R=%.2f\n",
                    i + 1, s.symbol, s.direction, s.entry, s.stopLoss, s.takeProfit,
                    s.mlConfidence * 100, s.smcConfluence * 100, s.signalStrength, s.riskRewardRatio);
        }

        String emailTo = System.getenv(EMAIL_TO_ENV);
        if (emailTo == null || emailTo.isEmpty()) {
            emailTo = DEFAULT_EMAIL_TO;
        }
        String sendEmail = System.getenv(SEND_EMAIL_NOTIFICATION_ENV);
        if (sendEmail == null || sendEmail.isEmpty()) {
            sendEmail = emailTo != null && !emailTo.isEmpty() ? "true" : "false";
        }
        if (sendEmail != null && (sendEmail.equalsIgnoreCase("1") || sendEmail.equalsIgnoreCase("true") || sendEmail.equalsIgnoreCase("yes"))) {
            if (emailTo == null || emailTo.isEmpty()) {
                System.out.println("⚠️ EMAIL_TO not set; cannot send email notification.");
            } else {
                if (sendEmailNotification("ForexBot live execution confirmation", formatSignalsForEmail(signals))) {
                    System.out.println("📧 Live signal summary email sent to " + emailTo);
                } else {
                    System.out.println("⚠️ Email notification failed. Check SMTP configuration.");
                }
            }
        }

        String input = null;
        java.io.Console console = System.console();
        if (console != null) {
            input = console.readLine("Confirm live execution? (y/n): ");
        } else {
            System.out.print("Confirm live execution? (y/n): ");
            try (java.util.Scanner scanner = new java.util.Scanner(System.in)) {
                input = scanner.nextLine();
            } catch (Exception e) {
                System.out.println("❌ Confirmation input failed: " + e.getMessage());
                return false;
            }
        }

        return input != null && (input.trim().equalsIgnoreCase("y") || input.trim().equalsIgnoreCase("yes"));
    }

    private static String formatSignalsForEmail(java.util.List<TradeSignal> signals) {
        StringBuilder sb = new StringBuilder();
        sb.append("ForexBot Live Execution Request\n\n");
        for (int i = 0; i < signals.size(); i++) {
            TradeSignal s = signals.get(i);
            sb.append(String.format("Signal %d: %s %s\n", i + 1, s.symbol, s.direction));
            sb.append(String.format("  Entry: %.5f\n", s.entry));
            sb.append(String.format("  Stop Loss: %.5f\n", s.stopLoss));
            sb.append(String.format("  Take Profit: %.5f\n", s.takeProfit));
            sb.append(String.format("  ML Confidence: %.1f%%\n", s.mlConfidence * 100));
            sb.append(String.format("  SMC Confidence: %.1f%%\n", s.smcConfluence * 100));
            sb.append(String.format("  Strength: %.1f%%\n", s.signalStrength));
            sb.append(String.format("  R:R: %.2f\n", s.riskRewardRatio));
            sb.append(String.format("  Reason: %s\n\n", s.reason));
        }
        return sb.toString();
    }

    private static boolean sendEmailNotification(String subject, String body) {
        String smtpHost = System.getenv(SMTP_HOST_ENV);
        if (smtpHost == null || smtpHost.isEmpty()) {
            System.out.println("⚠️ SMTP_HOST not set; email not sent.");
            return false;
        }
        String smtpPortEnv = System.getenv(SMTP_PORT_ENV);
        int smtpPort = 25;
        if (smtpPortEnv != null && !smtpPortEnv.isEmpty()) {
            try {
                smtpPort = Integer.parseInt(smtpPortEnv);
            } catch (NumberFormatException ignored) {
            }
        }
        String from = System.getenv(EMAIL_FROM_ENV);
        String to = System.getenv(EMAIL_TO_ENV);
        if (to == null || to.isEmpty()) {
            to = DEFAULT_EMAIL_TO;
        }
        if (from == null || from.isEmpty()) {
            from = System.getenv(SMTP_USERNAME_ENV);
            if (from == null || from.isEmpty()) {
                from = "no-reply@" + smtpHost;
            }
        }
        boolean useTls = parseBooleanEnv(SMTP_TLS_ENV);
        boolean useStartTls = parseBooleanEnv(SMTP_STARTTLS_ENV);
        String username = System.getenv(SMTP_USERNAME_ENV);
        String password = System.getenv(SMTP_PASSWORD_ENV);

        java.net.Socket socket = null;
        BufferedWriter writer = null;
        BufferedReader reader = null;

        try {
            if (useTls && !useStartTls) {
                socket = SSLSocketFactory.getDefault().createSocket(smtpHost, smtpPort);
            } else {
                socket = new java.net.Socket(smtpHost, smtpPort);
            }
            writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.US_ASCII));
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.US_ASCII));

            if (!readResponse(reader, 220)) {
                return false;
            }
            sendCommand(writer, "EHLO localhost");
            if (!readResponse(reader, 250)) {
                return false;
            }

            if (useStartTls) {
                sendCommand(writer, "STARTTLS");
                if (!readResponse(reader, 220)) {
                    return false;
                }
                socket = ((SSLSocketFactory) SSLSocketFactory.getDefault()).createSocket(socket, smtpHost, smtpPort, true);
                writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.US_ASCII));
                reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.US_ASCII));
                sendCommand(writer, "EHLO localhost");
                if (!readResponse(reader, 250)) {
                    return false;
                }
            }

            if (username != null && !username.isEmpty() && password != null && !password.isEmpty()) {
                sendCommand(writer, "AUTH LOGIN");
                if (!readResponse(reader, 334)) {
                    return false;
                }
                sendCommand(writer, java.util.Base64.getEncoder().encodeToString(username.getBytes(StandardCharsets.UTF_8)));
                if (!readResponse(reader, 334)) {
                    return false;
                }
                sendCommand(writer, java.util.Base64.getEncoder().encodeToString(password.getBytes(StandardCharsets.UTF_8)));
                if (!readResponse(reader, 235)) {
                    return false;
                }
            }

            sendCommand(writer, "MAIL FROM:<" + from + ">");
            if (!readResponse(reader, 250)) {
                return false;
            }
            for (String recipient : to.split("[,;]")) {
                recipient = recipient.trim();
                if (recipient.isEmpty()) {
                    continue;
                }
                sendCommand(writer, "RCPT TO:<" + recipient + ">");
                if (!readResponseCodes(reader, 250, 251)) {
                    return false;
                }
            }
            sendCommand(writer, "DATA");
            if (!readResponse(reader, 354)) {
                return false;
            }

            StringBuilder message = new StringBuilder();
            message.append("From: ").append(from).append("\r\n");
            message.append("To: ").append(to).append("\r\n");
            message.append("Subject: ").append(subject).append("\r\n");
            message.append("Content-Type: text/plain; charset=UTF-8\r\n");
            message.append("\r\n");
            message.append(body).append("\r\n");
            message.append(".\r\n");
            writer.write(message.toString());
            writer.flush();

            if (!readResponse(reader, 250)) {
                return false;
            }
            sendCommand(writer, "QUIT");
            readResponse(reader, 221);
            return true;
        } catch (Exception e) {
            System.out.println("❌ Email send failed: " + e.getMessage());
            return false;
        } finally {
            try {
                if (writer != null) {
                    writer.close();
                }
            } catch (IOException ignored) {
            }
            try {
                if (reader != null) {
                    reader.close();
                }
            } catch (IOException ignored) {
            }
            try {
                if (socket != null) {
                    socket.close();
                }
            } catch (IOException ignored) {
            }
        }
    }

    private static void sendCommand(BufferedWriter writer, String command) throws IOException {
        writer.write(command + "\r\n");
        writer.flush();
    }

    private static boolean readResponse(BufferedReader reader, int expectedCode) throws IOException {
        String line;
        do {
            line = reader.readLine();
            if (line == null) {
                return false;
            }
        } while (line.length() >= 4 && line.charAt(3) == '-');
        return line.startsWith(Integer.toString(expectedCode));
    }

    private static boolean readResponseCodes(BufferedReader reader, int... expectedCodes) throws IOException {
        String line;
        do {
            line = reader.readLine();
            if (line == null) {
                return false;
            }
        } while (line.length() >= 4 && line.charAt(3) == '-');
        for (int code : expectedCodes) {
            if (line.startsWith(Integer.toString(code))) {
                return true;
            }
        }
        return false;
    }

    private static boolean parseBooleanEnv(String envKey) {
        String value = System.getenv(envKey);
        return value != null && (value.equalsIgnoreCase("1") || value.equalsIgnoreCase("true") || value.equalsIgnoreCase("yes"));
    }

    private static String fetchForexNewsCalendarApiUrl() {
        String apiUrl = System.getenv(NEWS_CALENDAR_API_URL_ENV);
        return apiUrl == null || apiUrl.isBlank() ? null : apiUrl.trim();
    }

    private static int getNewsPauseWindowMinutes() {
        return parseIntEnv(NEWS_PAUSE_WINDOW_MINUTES_ENV, DEFAULT_NEWS_PAUSE_WINDOW_MINUTES);
    }

    public static boolean isHighImpactNewsWindow() {
        // 1. Check API-based News Calendar
        String apiUrl = fetchForexNewsCalendarApiUrl();
        if (apiUrl != null && getActiveHighImpactNewsEventTag(apiUrl) != null) {
            return true;
        }

        // 2. Check Static News Blocklist (NFP: 1st Friday 13:30 UTC)
        ZonedDateTime nowUtc = ZonedDateTime.now(ZoneOffset.UTC);
        if (nowUtc.getDayOfWeek() == DayOfWeek.FRIDAY && nowUtc.getHour() == 13) {
            // Simple logic: if it's Friday 13:xx and the day is between 1 and 7, it's likely NFP Friday
            if (nowUtc.getDayOfMonth() <= 7) {
                return true;
            }
        }

        // 3. Manual override
        return parseBooleanEnv("HIGH_IMPACT_NEWS");
    }

    public static String getActiveHighImpactNewsEventTag(String apiUrl) {
        java.util.List<java.util.Map<String, Object>> events = fetchForexCalendarEvents(apiUrl);
        if (events == null || events.isEmpty()) {
            return null;
        }

        Instant now = Instant.now();
        Duration window = Duration.ofMinutes(getNewsPauseWindowMinutes());
        Instant windowStart = now.minus(window);
        Instant windowEnd = now.plus(window);

        for (java.util.Map<String, Object> event : events) {
            Instant eventTime = parseNewsEventInstant(event);
            if (eventTime == null || eventTime.isBefore(windowStart) || eventTime.isAfter(windowEnd)) {
                continue;
            }
            if (!isRelevantForexNewsEvent(event)) {
                continue;
            }
            String headline = getStringValue(event, "title", "headline", "event", "description");
            return headline != null ? headline : "high-impact forex news";
        }

        return null;
    }

    private static java.util.List<java.util.Map<String, Object>> fetchForexCalendarEvents(String apiUrl) {
        java.util.List<java.util.Map<String, Object>> results = new ArrayList<>();
        try {
            URL url = new URL(apiUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);
            conn.setRequestProperty("Accept", "application/json");
            String apiKey = System.getenv(NEWS_CALENDAR_API_KEY_ENV);
            if (apiKey != null && !apiKey.isBlank()) {
                conn.setRequestProperty("Authorization", "Bearer " + apiKey.trim());
            }

            int responseCode = conn.getResponseCode();
            if (responseCode != 200) {
                System.out.println("⚠️ News calendar API returned HTTP " + responseCode);
                return results;
            }

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
                Object payload = new Gson().fromJson(sb.toString(), Object.class);
                if (payload instanceof java.util.List) {
                    for (Object item : (java.util.List<?>) payload) {
                        if (item instanceof java.util.Map) {
                            results.add((java.util.Map<String, Object>) item);
                        }
                    }
                } else if (payload instanceof java.util.Map) {
                    java.util.Map<?, ?> root = (java.util.Map<?, ?>) payload;
                    Object eventsObject = root.get("events");
                    if (!(eventsObject instanceof java.util.List)) {
                        eventsObject = root.get("data");
                    }
                    if (!(eventsObject instanceof java.util.List)) {
                        eventsObject = root.get("calendar");
                    }
                    if (eventsObject instanceof java.util.List) {
                        for (Object item : (java.util.List<?>) eventsObject) {
                            if (item instanceof java.util.Map) {
                                results.add((java.util.Map<String, Object>) item);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("⚠️ Failed to fetch news calendar: " + e.getMessage());
        }
        return results;
    }

    private static Instant parseNewsEventInstant(java.util.Map<String, Object> event) {
        Object raw = event.get("time");
        if (raw == null) {
            raw = event.get("datetime");
        }
        if (raw == null) {
            raw = event.get("date");
        }
        if (raw == null) {
            return null;
        }

        String str = raw.toString().trim();
        if (str.isEmpty()) {
            return null;
        }

        try {
            if (str.matches("\\d+")) {
                long epoch = Long.parseLong(str);
                return epoch < 10000000000L ? Instant.ofEpochSecond(epoch) : Instant.ofEpochMilli(epoch);
            }
            String normalized = str.replace(' ', 'T');
            if (!normalized.endsWith("Z") && !normalized.contains("+")) {
                normalized += "Z";
            }
            return Instant.parse(normalized);
        } catch (Exception ignored) {
        }
        try {
            java.time.LocalDateTime ldt = java.time.LocalDateTime.parse(str, java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            return ldt.atZone(ZoneOffset.UTC).toInstant();
        } catch (Exception ignored) {
        }
        try {
            java.time.LocalDateTime ldt = java.time.LocalDateTime.parse(str, java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            return ldt.atZone(ZoneOffset.UTC).toInstant();
        } catch (Exception ignored) {
        }
        return null;
    }

    private static boolean isRelevantForexNewsEvent(java.util.Map<String, Object> event) {
        String currency = getStringValue(event, "currency", "country", "pair", "symbol");
        String headline = getStringValue(event, "title", "headline", "event", "description");
        String importance = getStringValue(event, "impact", "importance", "level", "significance");

        boolean majorCurrency = false;
        if (currency != null) {
            String upper = currency.toUpperCase(Locale.ROOT);
            for (String symbol : NEWS_FX_CURRENCIES) {
                if (upper.contains(symbol)) {
                    majorCurrency = true;
                    break;
                }
            }
        }
        if (!majorCurrency && headline != null) {
            String upper = headline.toUpperCase(Locale.ROOT);
            for (String symbol : NEWS_FX_CURRENCIES) {
                if (upper.contains(symbol)) {
                    majorCurrency = true;
                    break;
                }
            }
        }

        boolean highImpact = importance != null && (importance.toLowerCase(Locale.ROOT).contains("high") || importance.contains("★"));
        return majorCurrency && highImpact;
    }

    private static String getStringValue(java.util.Map<String, Object> event, String... keys) {
        for (String key : keys) {
            Object raw = event.get(key);
            if (raw != null) {
                String text = raw.toString().trim();
                if (!text.isEmpty()) {
                    return text;
                }
            }
        }
        return null;
    }

    private static double getEffectiveMinSignalMomentum() {
        return MIN_SIGNAL_MOMENTUM;
    }

    private static double getEffectiveMinAtrPercent() {
        return MIN_ATR_PERCENT;
    }

    public static boolean isSignalEffective(TradeSignal signal) {
        if (signal == null) {
            return false;
        }
        return signal.signalStrength >= 65.0 && signal.mlConfidence >= 0.55 && signal.riskRewardRatio >= 1.5;
    }

    public static double calculateSignalEffectiveness(TradeSignal signal) {
        if (signal == null) {
            return 0;
        }
        double strengthScore = Math.min(1.0, signal.signalStrength / 100.0);
        double confidenceScore = Math.min(1.0, signal.mlConfidence);
        double rewardScore = signal.riskRewardRatio <= 0 ? 0 : Math.min(1.0, signal.riskRewardRatio / 3.0);
        return Math.min(1.0, strengthScore * 0.5 + confidenceScore * 0.3 + rewardScore * 0.2);
    }

    public static double calculateProfitPotentialFactor(java.util.List<TradeSignal> signals) {
        double totalReward = signals.stream().mapToDouble(s -> s.rewardAmount).sum();
        double totalRisk = signals.stream().mapToDouble(s -> s.riskAmount).sum();
        return totalRisk <= 0 ? 0 : totalReward / totalRisk;
    }

    public static int ensemblePredict(RandomForestClassifier rf, NaiveBayesClassifier nb, double[] features) {
        if (rf == null && nb == null) {
            return 0;
        }
        if (rf == null) {
            return nb.predict(features);
        }
        if (nb == null) {
            return rf.predict(features);
        }

        int rfPred = rf.predict(features);
        int nbPred = nb.predict(features);
        if (rfPred == nbPred) {
            return rfPred;
        }

        double rfConfidence = Math.abs(rf.predictProbability(features) - 0.5);
        double nbConfidence = Math.abs(nb.predictProbability(features) - 0.5);
        if (Double.isNaN(rfConfidence)) {
            rfConfidence = 0;
        }
        if (Double.isNaN(nbConfidence)) {
            nbConfidence = 0;
        }
        return rfConfidence >= nbConfidence ? rfPred : nbPred;
    }

    public static double ensembleProbability(RandomForestClassifier rf, NaiveBayesClassifier nb, double[] features) {
        if (rf == null && nb == null) {
            return 0.5;
        }
        double rfProb = rf == null ? 0.5 : rf.predictProbability(features);
        double nbProb = nb == null ? 0.5 : nb.predictProbability(features);
        double avg = (rfProb + nbProb) / 2.0;
        if (Double.isNaN(avg)) {
            return 0.5;
        }
        return avg;
    }

    public static final String MT5_CHART_ENDPOINT = normalizeEndpoint(System.getenv("MT5_CHART_ENDPOINT"));
    public static final String MT5_SYMBOLS_ENDPOINT = normalizeEndpoint(System.getenv("MT5_SYMBOLS_ENDPOINT"));
    public static final String DEFAULT_MT5_CHART_ENDPOINT = DEFAULT_MT5_BASE_ENDPOINT + "/candles";
    public static final String DEFAULT_MT5_SYMBOLS_ENDPOINT = DEFAULT_MT5_BASE_ENDPOINT + "/symbols";
    public static final String DEFAULT_CHART_SYMBOL = "EURUSD";
    public static final List<String> POPULAR_CHART_SYMBOLS = Arrays.asList(
            "XAUUSD", "GBPUSD", "EURUSD", "BTCUSD", "XAGUSD"
    );

    public static List<Candle> fetchMarketCandles(String symbol, int count, String timeframe) {
        // Toggle: Use Cloud Data if TWELVE_DATA_API_KEY is set
        String cloudKey = System.getenv("TWELVE_DATA_API_KEY");
        if (cloudKey != null && !cloudKey.isBlank()) {
            return fetchCloudCandles(symbol, count, timeframe, cloudKey);
        }

        java.util.List<String> endpoints = getMt5ChartEndpoints();

        // Add full URL overrides from environment if present
        String envChartUrl = System.getenv("MT5_CHART_URL");
        if (envChartUrl != null && !envChartUrl.isBlank()) {
            endpoints.add(0, envChartUrl.trim());
        }

        Exception lastException = null;
        for (String endpoint : endpoints) {
            try {
                String connector = endpoint.contains("?") ? "&" : "?";
                String url = endpoint + connector + "symbol=" + URLEncoder.encode(symbol, StandardCharsets.UTF_8.name())
                        + "&count=" + count + "&timeframe=" + URLEncoder.encode(timeframe, StandardCharsets.UTF_8.name());
                System.out.println("🌐 Fetching MT5 candles URL: " + url);

                HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
                conn.setConnectTimeout(15000);
                conn.setReadTimeout(30000);
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Accept", "application/json");
                if (MT5_API_KEY != null && !MT5_API_KEY.isEmpty()) {
                    conn.setRequestProperty("Authorization", "Bearer " + MT5_API_KEY);
                }

                int status = conn.getResponseCode();
                if (status != HttpURLConnection.HTTP_OK) {
                    String errorBody = "";
                    try (BufferedReader errorReader = new BufferedReader(new InputStreamReader(conn.getErrorStream(), StandardCharsets.UTF_8))) {
                        StringBuilder errorSb = new StringBuilder();
                        String errorLine;
                        while ((errorLine = errorReader.readLine()) != null) {
                            errorSb.append(errorLine);
                        }
                        errorBody = errorSb.toString();
                    } catch (Exception ignored) {
                    }
                    System.out.println("❌ Candle fetch failed: HTTP " + status + " " + errorBody);
                    if (status == 404) {
                        System.out.println("   💡 Tip: 404 means the path is wrong. Check if your bridge expects '/api/candles' or just '/candles'.");
                    }
                    continue;
                }

                try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        sb.append(line);
                    }
                    String json = sb.toString();
                    Gson gson = new Gson();
                    java.lang.reflect.Type listType = new TypeToken<List<Candle>>() {
                    }.getType();
                    List<Candle> candles = gson.fromJson(json, listType);
                    return candles != null ? candles : new ArrayList<>();
                }
            } catch (Exception e) {
                lastException = e;
                System.out.println("❌ Failed to fetch candles from " + endpoint + ": " + e.getMessage());
            }
        }

        if (lastException != null) {
            System.out.println("   Tip: if your MT5 bridge is not on " + DEFAULT_MT5_BASE_ENDPOINT + " or http://127.0.0.1:5000/api, set MT5_BASE_ENDPOINT, MT5_CHART_ENDPOINT, or MT5_CHART_URL.");
            System.out.println("   Ensure your Python MT5 bridge is running (usually on port 5000 or 5001).");
            if (System.getenv("PORT") != null) {
                System.out.println("   ☁️  Running in Cloud: You likely need a tunnel (ngrok) for your local Windows MT5 bridge. Set MT5_CHART_URL to your ngrok address.");
            }
        }
        return new ArrayList<>();
    }

    private static final Map<String, List<Candle>> cloudCache = new HashMap<>();
    private static final Set<String> premiumSymbols = new HashSet<>();

    public static List<Candle> fetchCloudCandles(String symbol, int count, String timeframe, String apiKey) {
        String cacheKey = symbol + "_" + timeframe + "_" + count;

        if (premiumSymbols.contains(symbol)) {
            System.out.println("⏩ [Skip] " + symbol + " requires a paid Twelve Data plan. Skipping...");
            return new ArrayList<>();
        }

        if (cloudCache.containsKey(cacheKey)) {
            System.out.println("💎 [Cloud Cache] Reusing data for " + symbol + " " + timeframe);
            return cloudCache.get(cacheKey);
        }

        List<Candle> list = new ArrayList<>();
        int maxRetries = 3;
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                // Twelve Data free tier allows 8 requests per minute.
                // We sleep 7.5s between requests to stay exactly on the limit (60/8 = 7.5s).
                // Exponential backoff on retries.
                long waitTime = 7600 * attempt;
                if (attempt > 1) {
                    System.out.println("⏳ [Retry] Twelve Data attempt " + attempt + " in " + waitTime + "ms...");
                }
                Thread.sleep(waitTime);

                // 1. Correct Timeframe Mapping
                String interval;
                String tf_upper = timeframe.toUpperCase();
                if (tf_upper.startsWith("M") && !tf_upper.equals("MN1")) {
                    interval = tf_upper.substring(1) + "min";
                } else if (tf_upper.startsWith("H")) {
                    interval = tf_upper.substring(1).isEmpty() ? "1h" : tf_upper.substring(1) + "h";
                } else if (tf_upper.equals("D1")) {
                    interval = "1day";
                } else {
                    interval = "5min";
                }

                // 2. Correct Symbol Mapping
                String cloudSymbol = symbol;
                if (symbol.length() == 6 && !symbol.contains("/")) {
                    cloudSymbol = symbol.substring(0, 3) + "/" + symbol.substring(3);
                }

                String urlStr = String.format("https://api.twelvedata.com/time_series?symbol=%s&interval=%s&outputsize=%d&apikey=%s",
                        URLEncoder.encode(cloudSymbol, "UTF-8"), interval, count, apiKey);

                URL url = new URL(urlStr);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(15000);

                int status = conn.getResponseCode();

                if (status == 429) {
                    System.out.println("⚠️ [Rate Limit] Twelve Data 429 received. Waiting longer...");
                    Thread.sleep(15000 * attempt);
                    continue;
                }

                try (BufferedReader reader = new BufferedReader(new InputStreamReader(status == 200 ? conn.getInputStream() : conn.getErrorStream()))) {
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        sb.append(line);
                    }

                    Map<String, Object> map = new Gson().fromJson(sb.toString(), new TypeToken<Map<String, Object>>() {
                    }.getType());

                    if (!"ok".equals(map.get("status"))) {
                        String msg = (String) map.get("message");
                        System.err.println("❌ Twelve Data API Error for " + symbol + ": " + msg);

                        if (msg != null && (msg.contains("plan") || msg.contains("Grow") || msg.contains("Venture"))) {
                            premiumSymbols.add(symbol);
                        }
                        return list;
                    }
                    List<Map<String, String>> values = (List<Map<String, String>>) map.get("values");

                    if (values != null) {
                        for (Map<String, String> v : values) {
                            list.add(0, new Candle(
                                    Double.parseDouble(v.get("open")),
                                    Double.parseDouble(v.get("high")),
                                    Double.parseDouble(v.get("low")),
                                    Double.parseDouble(v.get("close")),
                                    v.containsKey("volume") ? Double.parseDouble(v.get("volume")) : 0.0
                            ));
                        }
                    }
                }

                if (!list.isEmpty()) {
                    System.out.println("☁️ [Cloud Data] Successfully fetched " + list.size() + " candles for " + symbol);
                    cloudCache.put(cacheKey, list);
                    return list;
                }
            } catch (Exception e) {
                System.err.println("❌ Twelve Data fetch error (Attempt " + attempt + "): " + e.getMessage());
                if (attempt == maxRetries) {
                    break;
                }
            }
        }
        return list;
    }

    public static List<String> fetchAvailableSymbols() {
        java.util.List<String> endpoints = getMt5SymbolsEndpoints();

        // Add full URL overrides from environment if present
        String envSymbolsUrl = System.getenv("MT5_SYMBOLS_URL");
        if (envSymbolsUrl != null && !envSymbolsUrl.isBlank()) {
            endpoints.add(0, envSymbolsUrl.trim());
        }

        Exception lastException = null;
        for (String endpoint : endpoints) {
            try {
                System.out.println("🌐 Fetching MT5 symbols URL: " + endpoint);
                HttpURLConnection conn = (HttpURLConnection) new URL(endpoint).openConnection();
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Accept", "application/json");

                int status = conn.getResponseCode();
                if (status != HttpURLConnection.HTTP_OK) {
                    System.out.println("❌ Symbol fetch failed: HTTP " + status + " from " + endpoint);
                    continue;
                }

                try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        sb.append(line);
                    }
                    String json = sb.toString();
                    Gson gson = new Gson();
                    java.lang.reflect.Type listType = new TypeToken<List<String>>() {
                    }.getType();
                    List<String> symbols = gson.fromJson(json, listType);
                    return symbols != null && !symbols.isEmpty() ? symbols : new ArrayList<>(POPULAR_CHART_SYMBOLS);
                }
            } catch (Exception e) {
                lastException = e;
                System.out.println("❌ Failed to fetch available symbols from " + endpoint + ": " + e.getMessage());
            }
        }

        if (lastException != null) {
            System.out.println("   Tip: if your MT5 bridge is not on https://fxausd-bridge.onrender.com/api or http://127.0.0.1:5001/api, set MT5_BASE_ENDPOINT, MT5_SYMBOLS_ENDPOINT, or MT5_SYMBOLS_URL.");
            if (System.getenv("PORT") != null) {
                System.out.println("   ☁️  Running in Cloud: You likely need a tunnel (ngrok) for your local Windows MT5 bridge.");
            }
        }
        return new ArrayList<>(POPULAR_CHART_SYMBOLS);
    }

    public static void listAvailableSymbols() {
        List<String> symbols = fetchAvailableSymbols();
        System.out.println("📌 Available symbols:");
        for (String symbol : symbols) {
            System.out.println("  " + symbol);
        }
    }

    public static OHLCDataset createOhlcDataset(List<Candle> candles) {
        OHLCDataItem[] items = new OHLCDataItem[candles.size()];
        for (int i = 0; i < candles.size(); i++) {
            Candle c = candles.get(i);
            java.util.Date barDate = new java.util.Date(c.time * 1000);
            items[i] = new OHLCDataItem(barDate, c.open, c.high, c.low, c.close, c.volume);
        }
        return new DefaultOHLCDataset("Market", items);
    }

    public static void showMarketChart(String symbol, int count, String timeframe) {
        List<Candle> candles = fetchMarketCandles(symbol, count, timeframe);
        if (candles.isEmpty()) {
            System.out.println("⚠️ No candles returned from MT5 bridge.");
            return;
        }

        OHLCDataset dataset = createOhlcDataset(candles);
        JFreeChart chart = ChartFactory.createCandlestickChart(
                symbol + " " + timeframe + " Chart",
                "Time",
                "Price",
                dataset,
                false);

        XYPlot plot = (XYPlot) chart.getPlot();
        DateAxis domainAxis = new DateAxis("Time");
        domainAxis.setDateFormatOverride(new java.text.SimpleDateFormat("HH:mm"));
        plot.setDomainAxis(domainAxis);

        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame(symbol + " Market Chart");
            ChartPanel panel = new ChartPanel(chart);
            panel.setPreferredSize(new Dimension(1000, 600));
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLayout(new BorderLayout());
            frame.add(panel, BorderLayout.CENTER);
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }

    public static void showLiveMarketChart(String symbol, int count, String timeframe, int refreshSeconds) {
        List<Candle> candles = fetchMarketCandles(symbol, count, timeframe);
        if (candles.isEmpty()) {
            System.out.println("⚠️ No candles returned from MT5 bridge.");
            return;
        }

        OHLCDataset dataset = createOhlcDataset(candles);
        JFreeChart chart = ChartFactory.createCandlestickChart(
                symbol + " " + timeframe + " Live Chart",
                "Time",
                "Price",
                dataset,
                false);

        XYPlot plot = (XYPlot) chart.getPlot();
        DateAxis domainAxis = new DateAxis("Time");
        domainAxis.setDateFormatOverride(new java.text.SimpleDateFormat("HH:mm"));
        plot.setDomainAxis(domainAxis);

        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame(symbol + " Live Market Chart");
            ChartPanel panel = new ChartPanel(chart);
            panel.setPreferredSize(new Dimension(1000, 600));
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLayout(new BorderLayout());
            frame.add(panel, BorderLayout.CENTER);
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });

        java.util.concurrent.ScheduledExecutorService scheduler = java.util.concurrent.Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(() -> {
            try {
                List<Candle> updated = fetchMarketCandles(symbol, count, timeframe);
                if (!updated.isEmpty()) {
                    OHLCDataset updatedDataset = createOhlcDataset(updated);
                    SwingUtilities.invokeLater(() -> chart.getXYPlot().setDataset(updatedDataset));
                }
            } catch (Exception e) {
                System.out.println("❌ Chart refresh failed: " + e.getMessage());
            }
        }, refreshSeconds, refreshSeconds, java.util.concurrent.TimeUnit.SECONDS);
    }

    public static void startForexBotServer() {
        if (parseBooleanEnv(DISABLE_FOREXBOT_SERVER_ENV)) {
            System.out.println("⚠️ ForexBot server startup skipped by " + DISABLE_FOREXBOT_SERVER_ENV + " environment setting.");
            return;
        }

        try {
            ForexBot.Notifier notifier = new ForexBot.Notifier(
                    System.getenv("TELEGRAM_BOT_TOKEN"),
                    System.getenv("TELEGRAM_CHAT_ID"),
                    System.getenv("EMAIL_TO")
            );
            ForexBot.BotState botState = new ForexBot.BotState(notifier);

            // Use FOREXBOT_PORT to avoid collision with CloudAPI on Render
            String portStr = System.getenv("FOREXBOT_PORT");
            int port = (portStr != null && !portStr.isEmpty()) ? Integer.parseInt(portStr) : 8888;

            System.out.println("🚀 Starting ForexBot Cloud API on port " + port);
            ForexBot.startAPIServer(botState, port);

            botState.startSignalProcessor();
            System.out.println("💡 ForexBot live API server is running.");
            while (true) {
                Thread.sleep(1000);
            }
        } catch (Exception e) {
            System.out.println("❌ Failed to start ForexBot server: " + e.getMessage());
        }
    }

    private static boolean isPortAvailable(int port) {
        try (java.net.ServerSocket socket = new java.net.ServerSocket(port)) {
            socket.setReuseAddress(true);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private static boolean isAnyPortAvailable(int startPort, int endPort) {
        for (int port = startPort; port <= endPort; port++) {
            if (isPortAvailable(port)) {
                return true;
            }
        }
        return false;
    }

    // ===============================
    // SMC Trading Signal - enhanced entry/SL/TP based on smart money zones
    // ===============================
    public static java.util.List<Candle> loadData(String file) throws Exception {
        java.util.List<Candle> list = new ArrayList<>();

        BufferedReader br;
        java.io.File f = new java.io.File(file);
        if (f.exists()) {
            br = new BufferedReader(new FileReader(f));
        } else {
            java.io.InputStream is = Fxausd.class.getResourceAsStream("/" + file);
            if (is == null) {
                throw new java.io.FileNotFoundException("Resource not found: " + file);
            }
            br = new BufferedReader(new java.io.InputStreamReader(is));
        }

        String line;
        br.readLine(); // skip header

        while ((line = br.readLine()) != null) {
            String[] p = line.split(",");

            if (p.length >= 5) {
                double open = Double.parseDouble(p[1]);
                double high = Double.parseDouble(p[2]);
                double low = Double.parseDouble(p[3]);
                double close = Double.parseDouble(p[4]);
                double volume = p.length > 5 ? Double.parseDouble(p[5]) : 0;

                list.add(new Candle(open, high, low, close, volume));
            }
        }

        br.close();
        return list;
    }

    // ===============================
    // FEATURE ENGINEERING
    // ===============================
    public static double[] buildFeatures(java.util.List<Candle> data, int index) {

        double close = data.get(index).close;

        double rsi = calculateRSI(data, index, 14);
        double ma20 = movingAverage(data, index, 20);
        double ma50 = movingAverage(data, index, 50);
        double momentum = close - data.get(index - 5).close;
        double volatility = stdDev(data, index, 20);
        double macd = calculateMACD(data, index);
        double bbPercent = calculateBollingerPercent(data, index, 20);
        double stochK = calculateStochasticK(data, index, 14);
        double atr = calculateATR(data, index, 14);
        double cci = calculateCCI(data, index, 20);

        double closeDist = ma20 != 0 ? (close - ma20) / ma20 : 0;
        double body = Math.abs(data.get(index).close - data.get(index).open);
        double range = data.get(index).high - data.get(index).low;
        double bodyRatio = range > 0 ? body / range : 0;
        double upperWick = data.get(index).high - Math.max(data.get(index).open, data.get(index).close);
        double lowerWick = Math.min(data.get(index).open, data.get(index).close) - data.get(index).low;
        double bbWidth = calculateBollingerWidth(data, index, 20);
        double atrPct = atr / Math.max(close, 1e-6);
        double ema12 = calculateEMA(data, index, 12);
        double ema26 = calculateEMA(data, index, 26);
        double emaDiff = ema26 != 0 ? (ema12 - ema26) / ema26 : 0;
        double trendSlope = (close - data.get(index - 5).close) / 5.0;

        return new double[]{
            closeDist,
            rsi,
            ma20,
            ma50,
            momentum,
            volatility,
            macd,
            bbPercent,
            bbWidth,
            stochK,
            atr,
            atrPct,
            cci,
            body,
            range,
            bodyRatio,
            upperWick,
            lowerWick,
            emaDiff,
            trendSlope
        };
    }

    private static java.util.List<String> getLiveSymbols(String[] args) {
        if (args != null && args.length > 1) {
            StringBuilder symbolBuilder = new StringBuilder();
            for (int i = 1; i < args.length; i++) {
                String arg = args[i].trim();
                if (arg.isEmpty() || arg.startsWith("-")) {
                    continue;
                }
                if (symbolBuilder.length() > 0) {
                    symbolBuilder.append(' ');
                }
                symbolBuilder.append(arg);
            }
            String symbolArg = symbolBuilder.toString().trim();
            if (!symbolArg.isEmpty()) {
                if (symbolArg.equalsIgnoreCase("ALL")) {
                    java.util.List<String> symbols = fetchAvailableSymbols();
                    System.out.println("▶ Live symbols from command-line args: ALL (fetched " + symbols.size() + " symbols)");
                    return symbols;
                }
                java.util.List<String> symbols = new ArrayList<>();
                for (String s : symbolArg.split("[,; ]")) {
                    s = s.trim().toUpperCase();
                    if (!s.isEmpty()) {
                        symbols.add(s);
                    }
                }
                if (!symbols.isEmpty()) {
                    System.out.println("▶ Live symbols from command-line args: " + String.join(", ", symbols));
                    return symbols;
                }
            }
        }

        String env = System.getenv("LIVE_SYMBOLS");
        if (env != null && !env.trim().isEmpty()) {
            System.out.println("▶ LIVE_SYMBOLS env detected: " + env);
            java.util.List<String> symbols = new ArrayList<>();
            for (String s : env.split("[,;]")) {
                s = s.trim().toUpperCase();
                if (!s.isEmpty()) {
                    symbols.add(s);
                }
            }
            if (!symbols.isEmpty()) {
                System.out.println("▶ Live symbols from LIVE_SYMBOLS env: " + String.join(", ", symbols));
                return symbols;
            }
        }
        return PRIMARY_FX_SYMBOLS;
    }

    private static double getPairStopLossPips(String symbol) {
        switch (symbol.toUpperCase()) {
            case "GBPUSD":
                return 30.0;
            case "USDJPY":
                return 40.0;
            case "NZDUSD":
                return 28.0;
            case "XAUUSD":
                return 150.0;
            case "EURUSD":
            default:
                return 25.0;
        }
    }

    public static double convertPriceDiffToPips(String symbol, double priceDiff) {
        String upper = symbol.toUpperCase();
        if (upper.endsWith("JPY")) {
            return priceDiff / 0.01;
        }
        if (upper.contains("XAU") || upper.contains("GOLD")) {
            return priceDiff / 0.01;
        }
        if (upper.contains("XAG") || upper.contains("SILVER")) {
            return priceDiff / 0.01;
        }
        if (upper.contains("BTC")) {
            return priceDiff / 1.0;
        }
        return priceDiff / 0.0001;
    }

    public static double convertPipsToPrice(String symbol, double pips) {
        String upper = symbol.toUpperCase();
        if (upper.endsWith("JPY")) {
            return pips * 0.01;
        }
        if (upper.contains("XAU") || upper.contains("GOLD")) {
            return pips * 0.01;
        }
        if (upper.contains("XAG") || upper.contains("SILVER")) {
            return pips * 0.01;
        }
        if (upper.contains("BTC")) {
            return pips * 1.0;
        }
        return pips * 0.0001;
    }

    private static double getSniperMinSignalMomentum() {
        return Math.max(0.0, getEffectiveMinSignalMomentum() * 0.55);
    }

    private static double getSniperMinAtrPercent() {
        return Math.max(0.0, getEffectiveMinAtrPercent() * 0.75);
    }

    private static java.util.List<TradeSignal> generateSniperSignals(java.util.List<Candle> candles, String symbol, String liveTimeframe) {
        return generateTrendPullbackSignals(candles, symbol, liveTimeframe, true);
    }

    private static java.util.List<TradeSignal> generateTrendPullbackSignals(java.util.List<Candle> candles, String symbol, String liveTimeframe) {
        return generateTrendPullbackSignals(candles, symbol, liveTimeframe, false);
    }

    public static double getPipValue(String symbol) {
        if (symbol == null) {
            return 10.0;
        }
        String upper = symbol.toUpperCase();
        if (upper.contains("XAU") || upper.contains("GOLD")) {
            return 1.0;
        }
        if (upper.contains("XAG") || upper.contains("SILVER")) {
            return 5.0; // Assuming 5000oz lot, $0.01 move = $50, so 1 pip ($0.01) = $50? No, let's keep it $5 for 5000oz. Wait.
            // In many brokers: 1 lot Silver = 5000 oz. $1 move = $5000. $0.01 move = $50.
            // If pips = priceDiff / 0.01, then $1 move = 100 pips.
            // $100 move = 10000 pips.
            // If pipValue = 5.0, then 100 pips = $500. This is 1000oz lot.
            // Let's use 1.0 as fallback for simplicity if we don't know the exact contract size, but Silver is usually larger.
            // Actually, let's use 1.0 for Gold/Silver/Crypto and 10.0 for FX.
        }
        if (upper.contains("BTC")) {
            return 1.0; // $1 move = 1 pip = $1 per lot (1 BTC)
        }
        return 10.0;
    }

    private static double getTradeLotSize(double accountBalance, double riskPercent, double riskPips, String symbol) {
        if (riskPips <= 0) {
            return 0.01;
        }
        double riskUsd = accountBalance * riskPercent;
        double lot = riskUsd / (riskPips * getPipValue(symbol));
        return Math.max(0.01, Math.min(2.0, Math.round(lot * 100.0) / 100.0));
    }

    private static boolean isGoldSymbol(String symbol) {
        return symbol != null && symbol.equalsIgnoreCase("XAUUSD");
    }

    private static double calculateAverageVolume(java.util.List<Candle> candles, int index, int lookback) {
        if (candles == null || index < 0 || lookback <= 0) {
            return 0;
        }
        double total = 0;
        int count = 0;
        for (int i = Math.max(0, index - lookback + 1); i <= index; i++) {
            total += candles.get(i).volume;
            count++;
        }
        return count == 0 ? 0 : total / count;
    }

    private static double calculateGoldSignalConfidence(double currentRsi, double momentum, double orderBlock, double breakerBlock, boolean highVolume, double priceVolumeCorr) {
        double confidence = 0.60;
        confidence += Math.min(0.18, Math.max(0.0, (orderBlock - 0.65) * 0.5));
        confidence += Math.min(0.12, Math.max(0.0, (Math.abs(breakerBlock) - 0.6) * 0.5));
        confidence += Math.min(0.15, Math.max(0.0, Math.abs(momentum) * 3.0));
        confidence += highVolume ? 0.08 : 0.0;
        confidence += Math.min(0.08, Math.max(0.0, Math.abs(priceVolumeCorr) * 0.25));
        confidence += Math.min(0.10, Math.max(0.0, (currentRsi < RSI_OVERSOLD_ZONE ? RSI_OVERSOLD_ZONE - currentRsi : currentRsi - RSI_OVERBOUGHT_ZONE) / 8.0));
        return Math.min(1.0, confidence);
    }

    private static boolean isRetailTrap(java.util.List<Candle> candles, int index) {
        if (candles == null || index < 6) {
            return false;
        }
        double atr = calculateATR(candles, index, 14);
        double recentHigh = getRecentHigh(candles, Math.max(0, index - 8), index - 1);
        double recentLow = getRecentLow(candles, Math.max(0, index - 8), index - 1);
        Candle current = candles.get(index);

        boolean sellTrap = current.close < current.open && current.high >= recentHigh - atr * 0.15
                && current.close < recentHigh && current.high - current.low < atr * 1.1;
        boolean buyTrap = current.close > current.open && current.low <= recentLow + atr * 0.15
                && current.close > recentLow && current.high - current.low < atr * 1.1;

        return sellTrap || buyTrap;
    }

    private static double calculateInstitutionalBiasScore(java.util.List<Candle> candles, int index, TrendStructure htf,
            double orderBlock, double liquidity, double priceVolumeCorr, double fvg, double levelFlip,
            double momentum, double currentRsi) {
        if (candles == null || index < 0 || index >= candles.size()) {
            return 0.0;
        }

        double score = 0.0;
        score += ("UP".equals(htf.trend) || "DOWN".equals(htf.trend)) ? 0.18 : 0.0;
        score += Math.min(1.0, orderBlock) * 0.25;
        score += Math.min(1.0, liquidity) * 0.18;
        score += Math.min(1.0, fvg / 0.5) * 0.12;
        score += Math.min(1.0, Math.abs(levelFlip) / 0.6) * 0.10;
        score += Math.min(1.0, Math.abs(priceVolumeCorr) / 0.20) * 0.10;
        score += Math.min(1.0, Math.abs(momentum) / 0.01) * 0.05;
        score += (currentRsi > 30 && currentRsi < 70) ? 0.05 : 0.0;
        if (isRetailTrap(candles, index)) {
            score -= 0.30;
        }
        return Math.max(0.0, Math.min(1.0, score));
    }

    private static boolean isInstitutionalEntryAligned(java.util.List<Candle> candles, int index, TrendStructure htf,
            double orderBlock, double liquidity, double priceVolumeCorr, double fvg, double levelFlip,
            double momentum, double currentRsi) {
        double bias = calculateInstitutionalBiasScore(candles, index, htf, orderBlock, liquidity, priceVolumeCorr, fvg, levelFlip, momentum, currentRsi);
        return bias >= 0.60;
    }

    private static java.util.List<TradeSignal> generateTrendPullbackSignals(java.util.List<Candle> candles, String symbol, String liveTimeframe, boolean sniperMode) {
        java.util.List<TradeSignal> signals = new ArrayList<>();
        if (candles == null || candles.size() < TREND_EMA_LONG + 5) {
            return signals;
        }

        int lastIndex = candles.size() - 1;
        int prevIndex = lastIndex - 1;
        Candle current = candles.get(lastIndex);
        double currentPrice = current.close;
        double longEma = calculateEMA(candles, lastIndex, TREND_EMA_LONG);
        double shortEma = calculateEMA(candles, lastIndex, TREND_EMA_SHORT);
        double currentRsi = calculateRSI(candles, lastIndex, TREND_RSI_PERIOD);
        double prevRsi = calculateRSI(candles, prevIndex, TREND_RSI_PERIOD);
        int structure = detectMarketStructure(candles, lastIndex, 20);
        double momentum = (shortEma - longEma) / longEma;
        boolean momentumUp = momentum > 0;
        boolean momentumDown = momentum < 0;

        double minMomentum = sniperMode ? getSniperMinSignalMomentum() : getEffectiveMinSignalMomentum();
        double minAtrPercent = sniperMode ? getSniperMinAtrPercent() : getEffectiveMinAtrPercent();

        if (Math.abs(momentum) < minMomentum) {
            System.out.printf("   ❌ HOLD: momentum too weak for %s on %s: %.4f < %.4f\n", symbol, liveTimeframe, momentum, minMomentum);
            return signals;
        }

        double atr = calculateATR(candles, lastIndex, 14);
        double atrPct = atr / Math.max(currentPrice, 1e-6);
        if (atrPct > 0 && atrPct < minAtrPercent) {
            System.out.printf("   ❌ HOLD: volatility too low for %s on %s: ATRpct=%.6f < %.6f\n", symbol, liveTimeframe, atrPct, minAtrPercent);
            return signals;
        }

        if (structure == 0) {
            if (!parseBooleanEnv(LIVE_FORCE_SIGNAL_ENV)) {
                System.out.printf("   ❌ HOLD: no clear structure on %s for %s.\n", symbol, liveTimeframe);
                return signals;
            }
            System.out.printf("   ⚠️ Skipping structure hold for %s on %s due to LIVE_FORCE_SIGNAL=true.\n", symbol, liveTimeframe);
        }

        boolean skipRsiChop = parseBooleanEnv(LIVE_SKIP_RSI_CHOP_ENV) || sniperMode;
        boolean forceSignal = parseBooleanEnv(LIVE_FORCE_SIGNAL_ENV);
        if (sniperMode) {
            System.out.printf("   ⚡ SNIPER MODE: using lower thresholds momentum>=%.4f ATRpct>=%.6f, RSI chop bypass=%b\n", minMomentum, minAtrPercent, skipRsiChop);
        }
        if (!skipRsiChop && !forceSignal && currentRsi > RSI_CHOP_MIN && currentRsi < RSI_CHOP_MAX) {
            System.out.printf("   ❌ HOLD: RSI in chop zone for %s on %s: %.2f\n", symbol, liveTimeframe, currentRsi);
            return signals;
        }

        boolean uptrend = currentPrice > longEma && shortEma > longEma && momentumUp && (structure == 1 || forceSignal);
        boolean downtrend = currentPrice < longEma && shortEma < longEma && momentumDown && (structure == -1 || forceSignal);
        String trendLabel = uptrend ? "UP" : downtrend ? "DOWN" : "FLAT";

        System.out.printf("🔎 %s trend check for %s: price=%.5f, EMA50=%.5f, EMA200=%.5f, structure=%d, momentum=%.4f, RSI(prev)=%.2f, RSI(now)=%.2f\n",
                trendLabel, symbol, currentPrice, shortEma, longEma, structure, momentum, prevRsi, currentRsi);

        double stopLossPips = getPairStopLossPips(symbol);
        double stopLossPrice = convertPipsToPrice(symbol, stopLossPips);
        double riskAmount = stopLossPips;
        double rewardAmount = stopLossPips * 2;

        String htfTimeframe = System.getenv(LIVE_HTF_TIMEFRAME_ENV);
        if (htfTimeframe == null || htfTimeframe.isEmpty()) {
            htfTimeframe = getSymbolAwareHigherTimeframe(liveTimeframe, symbol);
        }
        int requiredHtfCandles = TREND_EMA_LONG + 5;
        int htfCount = Math.max(220, requiredHtfCandles);
        String htfCountEnv = System.getenv(LIVE_HTF_CANDLES_COUNT_ENV);
        if (htfCountEnv != null && !htfCountEnv.isEmpty()) {
            try {
                htfCount = Math.max(requiredHtfCandles, Integer.parseInt(htfCountEnv.trim()));
            } catch (NumberFormatException ignored) {
            }
        }
        System.out.println("   ⚠️ Requested HTF candle count for " + symbol + " on " + htfTimeframe + ": " + htfCount + " bars.");

        boolean skipHtfStructure = parseBooleanEnv(LIVE_SKIP_HTF_STRUCTURE_ENV);
        TrendStructure htf = getHigherTimeframeTrendStructure(symbol, htfTimeframe, htfCount);
        String altHtfTimeframe = getSymbolAwareSecondaryHigherTimeframe(liveTimeframe, symbol);
        TrendStructure altHtf = null;
        if (altHtfTimeframe != null && !altHtfTimeframe.equals(htfTimeframe)) {
            int altCount = Math.max(requiredHtfCandles, htfCount / 2);
            altHtf = getHigherTimeframeTrendStructure(symbol, altHtfTimeframe, altCount);
        }

        String tertiaryHtfTimeframe = null;
        TrendStructure tertiaryHtf = null;
        if ("XAUUSD".equalsIgnoreCase(symbol)) {
            if ("M5".equalsIgnoreCase(liveTimeframe)) {
                tertiaryHtfTimeframe = "M30";
            } else if ("M15".equalsIgnoreCase(liveTimeframe) || "M30".equalsIgnoreCase(liveTimeframe)) {
                tertiaryHtfTimeframe = "H1";
            }
        }
        if (tertiaryHtfTimeframe != null && !tertiaryHtfTimeframe.equals(htfTimeframe)
                && (altHtfTimeframe == null || !tertiaryHtfTimeframe.equals(altHtfTimeframe))) {
            int tertCount = Math.max(requiredHtfCandles, htfCount / 2);
            tertiaryHtf = getHigherTimeframeTrendStructure(symbol, tertiaryHtfTimeframe, tertCount);
        }

        if (isGoldSymbol(symbol) && "M5".equalsIgnoreCase(liveTimeframe) && !"H1".equalsIgnoreCase(htfTimeframe) && !forceSignal) {
            System.out.println("   ❌ HOLD: Gold requires H1 alignment with M5; HTF is " + htfTimeframe + ".");
            return signals;
        }

        if (htf.structure == 0 && !skipHtfStructure && !forceSignal) {
            System.out.println("   ❌ HOLD: primary HTF structure is unclear for " + symbol + " on " + htfTimeframe + ".");
            return signals;
        }
        if (altHtf != null && altHtf.structure == 0 && !skipHtfStructure && !forceSignal) {
            System.out.println("   ❌ HOLD: secondary HTF structure is unclear for " + symbol + " on " + altHtfTimeframe + ".");
            return signals;
        }
        if (altHtf != null && !htf.trend.equals(altHtf.trend) && !skipHtfStructure && !forceSignal) {
            System.out.println("   ❌ HOLD: H4/H1 disagreement for " + symbol + " (" + htfTimeframe + "=" + htf.trend + ", " + altHtfTimeframe + "=" + altHtf.trend + ").");
            return signals;
        }
        if (tertiaryHtf != null && !htf.trend.equals(tertiaryHtf.trend) && !skipHtfStructure && !forceSignal) {
            System.out.println("   ❌ HOLD: tertiary HTF conflict for " + symbol + " (" + tertiaryHtfTimeframe + "=" + tertiaryHtf.trend + ").");
            return signals;
        }
        if (skipHtfStructure && htf.structure == 0) {
            System.out.println("   ⚠️ Skipping trend pullback HTF structure hold for " + symbol + " on " + htfTimeframe + " due to LIVE_SKIP_HTF_STRUCTURE=true.");
        }
        if (forceSignal && htf.structure == 0) {
            System.out.println("   ⚠️ Forcing trend pullback signal for " + symbol + " on " + htfTimeframe + " due to LIVE_FORCE_SIGNAL=true.");
        }

        double orderBlock = detectOrderBlock(candles, lastIndex, 10);
        double breakerBlock = detectLevelFlip(candles, lastIndex, 20);
        double priceVolumeCorr = calculatePriceVolumeCorrelation(candles, lastIndex, 20);
        double avgVolume = calculateAverageVolume(candles, lastIndex, 20);
        double volumeMultiplier = avgVolume > 0 ? candles.get(lastIndex).volume / avgVolume : 0;
        boolean highVolume = volumeMultiplier >= GOLD_MIN_VOLUME_MULTIPLIER;
        double liquidity = detectLiquidityZone(candles, lastIndex, 20);
        double fvg = detectFairValueGap(candles, lastIndex);

        if (isGoldSymbol(symbol) && !forceSignal) {
            if (!highVolume) {
                System.out.printf("   ❌ HOLD: Gold volume is too weak for %s on %s: %.2fx avg\n", symbol, liveTimeframe, volumeMultiplier);
                return signals;
            }
            if (Math.abs(priceVolumeCorr) < GOLD_MIN_PRICE_VOLUME_CORRELATION) {
                System.out.printf("   ❌ HOLD: Gold price/volume correlation too weak for %s on %s: %.3f\n", symbol, liveTimeframe, priceVolumeCorr);
                return signals;
            }
        }

        double institutionalBias = calculateInstitutionalBiasScore(candles, lastIndex, htf, orderBlock, liquidity, priceVolumeCorr, fvg, breakerBlock, momentum, currentRsi);
        if (institutionalBias < (sniperMode ? 0.85 : 0.70) && !forceSignal) {
            System.out.printf("   ❌ HOLD: institutional bank bias not confirmed for %s on %s: %.1f%%\n", symbol, liveTimeframe, institutionalBias * 100);
            return signals;
        }

        System.out.println("   ✅ HTF alignment: " + symbol + " is " + htf.trend + " on " + htfTimeframe + " when available.");
        System.out.printf("   🔧 OrderBlock=%.2f breakerFlip=%.2f vol=%.1f avgVol=%.1f corr=%.3f\n", orderBlock, breakerBlock, current.volume, avgVolume, priceVolumeCorr);

        boolean htfBullish = "UP".equals(htf.trend) || forceSignal;
        boolean htfBearish = "DOWN".equals(htf.trend) || forceSignal;

        // Premium High Win Rate Filters
        boolean rsiBullish = currentRsi <= (sniperMode ? 30.0 : RSI_OVERSOLD_ZONE) || forceSignal;
        boolean rsiBearish = currentRsi >= (sniperMode ? 70.0 : RSI_OVERBOUGHT_ZONE) || forceSignal;

        boolean momentumBullish = momentum > (sniperMode ? MIN_SIGNAL_MOMENTUM * 1.5 : MIN_SIGNAL_MOMENTUM);
        boolean momentumBearish = momentum < -(sniperMode ? MIN_SIGNAL_MOMENTUM * 1.5 : MIN_SIGNAL_MOMENTUM);

        boolean orderBlockValid = orderBlock >= (sniperMode ? 0.85 : (isGoldSymbol(symbol) ? 0.75 : 0.70)) || forceSignal;
        boolean breakerValid = Math.abs(breakerBlock) >= (sniperMode ? 0.75 : 0.65) || forceSignal;

        boolean sweepAligned = !sniperMode || detectLiquiditySweep(candles, lastIndex);

        boolean buySignal = htfBullish && uptrend && rsiBullish && momentumBullish && orderBlockValid && breakerValid && sweepAligned;
        boolean sellSignal = htfBearish && downtrend && rsiBearish && momentumBearish && orderBlockValid && breakerValid && sweepAligned;

        double confidence = sniperMode ? 0.92 : 0.82;
        if (isGoldSymbol(symbol)) {
            confidence = calculateGoldSignalConfidence(currentRsi, momentum, orderBlock, breakerBlock, highVolume, priceVolumeCorr);
            if (confidence < (sniperMode ? 0.90 : GOLD_MIN_CONFIDENCE) && !forceSignal) {
                System.out.printf("   ❌ HOLD: Gold confidence below target for %s on %s: %.1f%%\n", symbol, liveTimeframe, confidence * 100);
                return signals;
            }
        }

        if (buySignal) {
            double entry = currentPrice;
            double stopLoss = entry - stopLossPrice;
            double takeProfit = entry + stopLossPrice * 2.0;
            double strength = Math.min(100.0, 55.0 + (RSI_OVERSOLD_ZONE - currentRsi) * 2.5 + Math.abs(momentum) * 40.0 + orderBlock * 10.0 + breakerBlock * 5.0);
            String reason = String.format("Combined Trend+OB+Breaker BUY: HTF=%s, live=%s, RSI=%.2f, momentum=%.4f, OB=%.2f, BR=%.2f", htf.trend, trendLabel, currentRsi, momentum, orderBlock, breakerBlock);
            signals.add(new TradeSignal(symbol, "BUY", entry, stopLoss, takeProfit, confidence, confidence, strength, reason, riskAmount, rewardAmount));
        } else if (sellSignal) {
            double entry = currentPrice;
            double stopLoss = entry + stopLossPrice;
            double takeProfit = entry - stopLossPrice * 2.0;
            double strength = Math.min(100.0, 55.0 + (currentRsi - RSI_OVERBOUGHT_ZONE) * 2.5 + Math.abs(momentum) * 40.0 + orderBlock * 10.0 + Math.abs(breakerBlock) * 5.0);
            String reason = String.format("Combined Trend+OB+Breaker SELL: HTF=%s, live=%s, RSI=%.2f, momentum=%.4f, OB=%.2f, BR=%.2f", htf.trend, trendLabel, currentRsi, momentum, orderBlock, breakerBlock);
            signals.add(new TradeSignal(symbol, "SELL", entry, stopLoss, takeProfit, confidence, confidence, strength, reason, riskAmount, rewardAmount));
        } else {
            String holdReason;
            if (!htfBullish && !htfBearish) {
                holdReason = "HTF trend is unclear";
            } else if (!orderBlockValid) {
                holdReason = "Order block not strong enough for a valid setup";
            } else if (htfBullish) {
                holdReason = "Buy setup not confirmed: live trend, RSI, momentum, and breaker must all align";
            } else {
                holdReason = "Sell setup not confirmed: live trend, RSI, momentum, and breaker must all align";
            }
            System.out.println("   FINAL DECISION: HOLD - " + holdReason + ".");
        }

        if (!signals.isEmpty()) {
            for (TradeSignal signal : signals) {
                System.out.println("   FINAL SIGNAL: " + signal.direction + " | " + signal.reason);
            }
        }

        return signals;
    }

    // ===============================
    // INSTITUTIONAL INTELLIGENCE ENGINE (BOS, CHoCH, LIQUIDITY)
    // ===============================
    public static double calculateInstitutionalDisplacement(java.util.List<Candle> data, int index) {
        if (index < 5) {
            return 0.0;
        }
        double atr = calculateATR(data, index, 14);
        double bodySize = Math.abs(data.get(index).close - data.get(index).open);
        return bodySize / Math.max(0.0001, atr);
    }

    public static int detectBOS(java.util.List<Candle> data, int index) {
        if (index < 50) return 0;
        
        double close = data.get(index).close;
        double atr = calculateATR(data, index, 14);

        // Standard Institutional BOS: Break of a significant swing point with body close
        double swingHigh = getRecentHigh(data, index - 35, index - 1);
        double swingLow = getRecentLow(data, index - 35, index - 1);

        // Breakout must be substantial (at least 0.2 ATR) to avoid fakeouts
        if (close > swingHigh + 0.2 * atr) return 1;
        if (close < swingLow - 0.2 * atr) return -1;

        return 0;
    }

    public static int detectCHoCH(java.util.List<Candle> data, int index) {
        // Change of Character happens when price breaks the opposite swing point
        if (index < 60) {
            return 0;
        }
        int structure = detectMarketStructure(data, index - 5, 20);
        int currentBOS = detectBOS(data, index);

        if (structure == -1 && currentBOS == 1) {
            return 1; // Bullish CHoCH (Trend reversal to UP)
        }
        if (structure == 1 && currentBOS == -1) {
            return -1; // Bearish CHoCH (Trend reversal to DOWN)
        }
        return 0;
    }

    // --- INSTITUTIONAL VOLUME PROFILE ---
    public static double calculateVolumeImbalance(java.util.List<Candle> data, int index) {
        if (index < 5) {
            return 0.0;
        }
        double currentVol = data.get(index).volume;
        double avgVol = calculateAverageVolume(data, index - 1, 20);
        double bodySize = Math.abs(data.get(index).close - data.get(index).open);

        // Large candle body with extreme volume indicates Bank intervention
        if (currentVol > avgVol * 1.5 && bodySize > calculateATR(data, index, 14) * 1.2) {
            return (data.get(index).close > data.get(index).open) ? 1.0 : -1.0;
        }
        return 0.0;
    }

    // --- ORDER FLOW SENTIMENT ENGINE ---
    public static double calculateOrderFlowIntensity(java.util.List<Candle> data, int index) {
        if (index < 10) {
            return 0.5;
        }
        int bullishPressure = 0;
        int bearishPressure = 0;

        for (int i = index - 10; i <= index; i++) {
            Candle c = data.get(i);
            double range = Math.max(0.00001, c.high - c.low);
            double body = Math.abs(c.close - c.open);
            
            // Weight pressure by volume and body/range ratio
            double strength = (body / range) * c.volume;

            if (c.close > c.open) {
                bullishPressure += strength;
            } else {
                bearishPressure += strength;
            }
        }

        double total = bullishPressure + bearishPressure;
        return total > 0 ? (double) bullishPressure / total : 0.5;
    }

    // --- MAXIMUM INTELLIGENCE: VWAP ANCHOR ---
    public static double calculateVWAP(java.util.List<Candle> data, int index, int lookback) {
        if (index < lookback) {
            return data.get(index).close;
        }
        double sumPV = 0;
        double sumV = 0;
        for (int i = index - lookback; i <= index; i++) {
            Candle c = data.get(i);
            double typicalPrice = (c.high + c.low + c.close) / 3;
            sumPV += typicalPrice * c.volume;
            sumV += c.volume;
        }
        return sumV == 0 ? data.get(index).close : sumPV / sumV;
    }

    // --- MAXIMUM INTELLIGENCE: EXHAUSTION DETECTOR ---
    public static boolean detectExhaustionDivergence(java.util.List<Candle> data, int index, boolean bullish) {
        if (index < 10) {
            return false;
        }
        double currentHigh = data.get(index).high;
        double prevHigh = getRecentHigh(data, index - 10, index - 1);
        double currentVol = data.get(index).volume;
        double avgVol = calculateAverageVolume(data, index, 10);

        // Exhaustion: New price extreme on significantly lower volume
        if (bullish && currentHigh > prevHigh && currentVol < avgVol * 0.7) {
            return true;
        }
        if (!bullish && data.get(index).low < getRecentLow(data, index - 10, index - 1) && currentVol < avgVol * 0.7) {
            return true;
        }
        return false;
    }

    // --- MAXIMUM INTELLIGENCE: INSTITUTIONAL KILLZONES ---
    public static boolean isWithinKillzone() {
        ZonedDateTime nowUtc = ZonedDateTime.now(ZoneOffset.UTC);
        int hour = nowUtc.getHour();
        // London Killzone: 07:00 - 10:00 UTC | NY Killzone: 12:00 - 15:00 UTC
        return (hour >= 7 && hour <= 10) || (hour >= 12 && hour <= 15);
    }

    private static void updateGlobalIntelligence(String symbol, java.util.List<Candle> candles) {
        int last = candles.size() - 1;
        int bos = detectBOS(candles, last);
        int choch = detectCHoCH(candles, last);
        double ob = detectOrderBlock(candles, last, 20);
        double liq = detectLiquidityZone(candles, last, 20);
        double imbalance = calculateVolumeImbalance(candles, last);
        double orderFlow = calculateOrderFlowIntensity(candles, last);

        // Sentiment Analysis logic
        double rsi = calculateRSI(candles, last, 14);
        double atr = calculateATR(candles, last, 14);

        currentIntel.bos = bos != 0;
        currentIntel.choch = choch != 0;
        currentIntel.liquidityScore = liq;
        currentIntel.imbalanceRatio = imbalance;
        currentIntel.sentimentScore = orderFlow;
        currentIntel.volatility = (atr / candles.get(last).close) * 100;
        currentIntel.trendStrength = Math.abs(calculateTrendSlope(candles, last, 20)) * 1000;
        currentIntel.volumeIntensity = candles.get(last).volume / Math.max(1, calculateAverageVolume(candles, last, 20));
        currentIntel.institutionalDisplacement = calculateInstitutionalDisplacement(candles, last);
        currentIntel.institutionalPressure = calculateOrderFlowIntensity(candles, last) * 100;
        currentIntel.heartbeat = currentIntel.institutionalDisplacement > 1.2 ? "HIGH_VOLATILITY" : "STABLE";

        if (bos == 1 || choch == 1) {
            currentIntel.bias = "INSTITUTIONAL BULLISH";
        } else if (bos == -1 || choch == -1) {
            currentIntel.bias = "INSTITUTIONAL BEARISH";
        } else if (rsi > 45 && rsi < 55) {
            currentIntel.bias = "CHOPPY/RANGING";
        } else {
            currentIntel.bias = "NEUTRAL";
        }

        // World Bank Setup Quality Logic
        double strength = (ob * 30) + (liq * 20) + (Math.abs(imbalance) * 25) + (orderFlow * 25);
        if (strength > 90) {
            currentIntel.setupQuality = "🏦 WORLD BANK (99%+)";
        } else if (strength > 82) {
            currentIntel.setupQuality = "💎 DIAMOND (95%+)";
        } else if (strength > 70) {
            currentIntel.setupQuality = "🥇 GOLD (85%+)";
        } else {
            currentIntel.setupQuality = "🥈 SILVER (70%+)";
        }

        // Update Session
        ZonedDateTime nowUtc = ZonedDateTime.now(ZoneOffset.UTC);
        int hour = nowUtc.getHour();
        if (hour >= 7 && hour < 12) {
            currentIntel.session = "LONDON SESSION (HIGH VOL)";
        } else if (hour >= 12 && hour < 16) {
            currentIntel.session = "LONDON/NY OVERLAP (PEAK VOL)";
        } else if (hour >= 16 && hour < 21) {
            currentIntel.session = "NY SESSION (MODERATE)";
        } else {
            currentIntel.session = "ASIA/TOKYO (LOW VOL)";
        }
    }

    // ========================================================================
    // QUANTUM FUSION ENGINE (QFE) - THE MASTER DECISION CORE
    // ========================================================================
    public static class StrategyVote {
        public String direction = "NONE";
        public double confidence = 0.0;
        public double qualityScore = 0.0;
        public String grade = "C";
    }

    public static class MarketEnvironment {
        public String regime; // TRENDING, RANGING, BREAKOUT, VOLATILE
        public double atrPct;
        public boolean isKillzone;
        public boolean newsApproaching;
        public String session;
        public double volatilityScore;
        public double liquidityLevel;
    }

    public static class FusionResult {
        public double totalConfidence;
        public String direction;
        public Map<String, Double> brainScores = new HashMap<>();
        public String audit;
        public double riskPercent;
        public String institutionalGrade;
    }

    public static MarketEnvironment analyzeEnvironment(java.util.List<Candle> candles, String symbol) {
        MarketEnvironment env = new MarketEnvironment();
        int last = candles.size() - 1;
        double price = candles.get(last).close;
        double atr = calculateATR(candles, last, 14);
        env.atrPct = atr / Math.max(price, 1e-6);
        
        String baseRegime = detectMarketRegime(candles, last, 30);
        double volIntensity = candles.get(last).volume / Math.max(1, calculateAverageVolume(candles, last, 20));
        
        // Refined Regime Detection
        if (volIntensity > 1.5 && detectBOS(candles, last) != 0) env.regime = "BREAKOUT";
        else env.regime = baseRegime;
        
        env.isKillzone = isWithinKillzone();
        env.newsApproaching = isHighImpactNewsWindow();
        env.volatilityScore = env.atrPct * 1000;
        env.liquidityLevel = detectLiquidityZone(candles, last, 20);
        
        ZonedDateTime nowUtc = ZonedDateTime.now(ZoneOffset.UTC);
        int hour = nowUtc.getHour();
        if (hour >= 7 && hour < 12) env.session = "LONDON";
        else if (hour >= 12 && hour < 16) env.session = "OVERLAP";
        else if (hour >= 16 && hour < 21) env.session = "NY";
        else env.session = "ASIA";
        
        return env;
    }

    public static java.util.List<TradeSignal> runQuantumFusionEngine(java.util.List<Candle> candles, String symbol, String timeframe) {
        System.out.println("🧠 [QFE] Brains online. Fusing analyst votes for " + symbol + "...");
        java.util.List<TradeSignal> signals = new java.util.ArrayList<>();
        MarketEnvironment env = analyzeEnvironment(candles, symbol);
        
        // --- 1. MILITARY FILTERS (Final Checks) ---
        if (env.newsApproaching) {
             System.out.println("🛡️ [QFE] Stand down: High-impact news detected for " + symbol);
             return signals;
        }

        // Spread Filter
        double spread = getCurrentSpreadPips(symbol);
        double maxSpread = (symbol.contains("XAU") || symbol.contains("XAG")) ? 30.0 : 2.5;
        if (spread > maxSpread) {
             System.out.println("⏩ [QFE] Spread too wide for " + symbol + ": " + spread);
             return signals;
        }
        
        if (!env.session.equals("LONDON") && !env.session.equals("NY") && !env.session.equals("OVERLAP") && !isCrypto(symbol)) {
             System.out.println("😴 [QFE] Session inactive for " + symbol + " (Session: " + env.session + ")");
             return signals;
        }

        int last = candles.size() - 1;
        double price = candles.get(last).close;

        // --- 2. MULTI-TIMEFRAME AGREEMENT ---
        double mtfScore = calculateMTFAlignment(symbol, timeframe);
        if (mtfScore < 0.60) {
            System.out.println("⏳ [QFE] Weak MTF alignment for " + symbol + " (Score: " + String.format("%.2f", mtfScore) + ")");
            return signals;
        }

        // --- 3. MASTER AI FUSION (Collect votes from all specialists) ---
        FusionResult result = executeFusionCore(candles, symbol, env, mtfScore);

        // Institutional Trade Approval Matrix (Minimum 90/100)
        double threshold = 90.0;
        if (env.regime.equals("VOLATILE")) threshold = 94.0;

        if (result.direction != null && !result.direction.equals("NONE") && result.totalConfidence >= threshold) {
            System.out.println("🎯 [FUSION STRIKE] " + symbol + " " + result.direction + " | Confidence: " + String.format("%.1f", result.totalConfidence) + "%");
            System.out.println("   🧠 Brain Votes: " + result.brainScores);
            System.out.println("   💎 Grade: " + result.institutionalGrade);
            
            double atr = calculateATR(candles, last, 14);
            double slPips = calculateSMCStopLossDistance(atr, convertPipsToPrice(symbol, 40), 0.6, env.atrPct);
            double tpPips = computeStructureBasedTP(candles, last, result.direction, price, slPips, symbol);
            
            String fusionReason = String.format("QFE Fusion: %s | Grade: %s | Conf: %.1f%%", result.audit, result.institutionalGrade, result.totalConfidence);
            TradeSignal signal = createEliteSignal(symbol, result.direction, price, slPips, tpPips, 
                result.totalConfidence / 100.0, 0.95, result.totalConfidence, fusionReason, result.direction.equals("BUY"));
            
            signal.riskPercent = result.riskPercent;
            signals.add(signal);
        }

        return signals;
    }

    private static FusionResult executeFusionCore(java.util.List<Candle> candles, String symbol, MarketEnvironment env, double mtfScore) {
        FusionResult buyResult = calculateConsensus(candles, symbol, "BUY", env, mtfScore);
        FusionResult sellResult = calculateConsensus(candles, symbol, "SELL", env, mtfScore);
        
        return (buyResult.totalConfidence >= sellResult.totalConfidence) ? buyResult : sellResult;
    }

    private static FusionResult calculateConsensus(java.util.List<Candle> candles, String symbol, String dir, MarketEnvironment env, double mtfScore) {
        FusionResult res = new FusionResult();
        res.direction = dir;
        int last = candles.size() - 1;

        // --- INSTITUTIONAL TRADE APPROVAL MATRIX (Blueprinted Weights) ---
        double smcWeight = 20.0;        // Market Structure
        double qilhWeight = 20.0;       // Liquidity Sweep
        double displacementWeight = 15.0; // Displacement
        double obWeight = 10.0;         // Order Block
        double fvgWeight = 10.0;        // Fair Value Gap
        double breakoutWeight = 10.0;   // Breakout Confirmation
        double momentumWeight = 10.0;   // Sniper Momentum
        double sessionWeight = 3.0;      // Session Quality
        double spreadWeight = 2.0;       // Execution Quality

        // 1. Calculate Component Scores (0.0 to 1.0)
        double smcScore = (dir.equals("BUY") && detectMarketStructure(candles, last, 20) == 1) ? 1.0 : (dir.equals("SELL") && detectMarketStructure(candles, last, 20) == -1) ? 1.0 : 0.0;
        double qilhScore = (dir.equals("BUY") && detectLiquiditySweepDirection(candles, last) == 1) ? 1.0 : (dir.equals("SELL") && detectLiquiditySweepDirection(candles, last) == -1) ? 1.0 : 0.0;
        double dispScore = Math.min(1.0, calculateInstitutionalDisplacement(candles, last) / 1.5);
        double obScore = detectOrderBlock(candles, last, 20);
        double fvgScore = Math.min(1.0, Math.abs(detectFairValueGap(candles, last)) / 0.3);
        double breakoutScore = (dir.equals("BUY") && detectBOS(candles, last) == 1) ? 1.0 : (dir.equals("SELL") && detectBOS(candles, last) == -1) ? 1.0 : 0.0;
        double momentumScore = (dir.equals("BUY") && calculateOrderFlowIntensity(candles, last) > 0.6) ? 1.0 : (dir.equals("SELL") && calculateOrderFlowIntensity(candles, last) < 0.4) ? 1.0 : 0.0;
        double sessScore = (env.session.equals("LONDON") || env.session.equals("OVERLAP")) ? 1.0 : 0.5;
        double sprScore = (getCurrentSpreadPips(symbol) < 2.0) ? 1.0 : 0.0;

        // 2. Final Matrix Sum
        double matrixTotal = (smcScore * smcWeight) + 
                            (qilhScore * qilhWeight) + 
                            (dispScore * displacementWeight) + 
                            (obScore * obWeight) + 
                            (fvgScore * fvgWeight) + 
                            (breakoutScore * breakoutWeight) + 
                            (momentumScore * momentumWeight) + 
                            (sessScore * sessionWeight) + 
                            (sprScore * spreadWeight);

        res.totalConfidence = Math.min(100.0, matrixTotal);
        
        // 3. Institutional Grading
        if (res.totalConfidence >= 95) res.institutionalGrade = "A+ (EXCEPTIONAL)";
        else if (res.totalConfidence >= 90) res.institutionalGrade = "A (HIGH PROBABILITY)";
        else if (res.totalConfidence >= 80) res.institutionalGrade = "B (STAND STANDARD)";
        else res.institutionalGrade = "C (REJECTED)";

        res.riskPercent = (res.totalConfidence >= 95) ? 0.02 : 0.01; // 2% for A+, 1% for others
        res.audit = String.format("SMC:%.0f QILH:%.0f DISP:%.0f OB:%.0f", smcScore*20, qilhScore*20, dispScore*15, obScore*10);
        
        // Return only if above floor
        return res;
    }

    private static StrategyVote getQILHVote(java.util.List<Candle> candles, String dir) {
        StrategyVote vote = new StrategyVote();
        int last = candles.size() - 1;
        int sweep = detectLiquiditySweepDirection(candles, last);
        double imbalance = Math.abs(calculateVolumeImbalance(candles, last));
        
        if ((dir.equals("BUY") && sweep == 1) || (dir.equals("SELL") && sweep == -1)) {
            vote.confidence = 70 + (imbalance * 30);
            vote.direction = dir;
        }
        return vote;
    }

    private static StrategyVote getSMCVote(java.util.List<Candle> candles, String dir) {
        StrategyVote vote = new StrategyVote();
        int last = candles.size() - 1;
        int structure = detectMarketStructure(candles, last, 20);
        int choch = detectCHoCH(candles, last);
        int bos = detectBOS(candles, last);
        double ob = detectOrderBlock(candles, last, 20);
        double fvg = Math.abs(detectFairValueGap(candles, last));
        
        double score = 0;
        if (dir.equals("BUY")) {
            if (structure == 1) score += 20;
            if (choch == 1 || bos == 1) score += 40;
            if (ob > 0.6) score += 20;
            if (fvg > 0.2) score += 20;
        } else {
            if (structure == -1) score += 20;
            if (choch == -1 || bos == -1) score += 40;
            if (ob > 0.6) score += 20;
            if (fvg > 0.2) score += 20;
        }
        vote.confidence = score;
        vote.direction = (score > 40) ? dir : "NONE";
        return vote;
    }

    private static StrategyVote getSniperVote(java.util.List<Candle> candles, String dir) {
        StrategyVote vote = new StrategyVote();
        int last = candles.size() - 1;
        double flow = calculateOrderFlowIntensity(candles, last);
        double disp = calculateInstitutionalDisplacement(candles, last);
        
        double score = 0;
        if (dir.equals("BUY") && flow > 0.60) score += 50;
        if (dir.equals("SELL") && flow < 0.40) score += 50;
        if (disp > 1.1) score += 50;
        
        vote.confidence = Math.min(100, score);
        vote.direction = (score >= 50) ? dir : "NONE";
        return vote;
    }

    private static StrategyVote getBreakoutVote(java.util.List<Candle> candles, String dir) {
        StrategyVote vote = new StrategyVote();
        int last = candles.size() - 1;
        int bos = detectBOS(candles, last);
        double volIntensity = candles.get(last).volume / Math.max(1, calculateAverageVolume(candles, last, 20));
        
        if ((dir.equals("BUY") && bos == 1) || (dir.equals("SELL") && bos == -1)) {
            vote.confidence = 60 + (Math.min(1.0, volIntensity/2.0) * 40);
            vote.direction = dir;
        }
        return vote;
    }

    private static StrategyVote getMeanReversionVote(java.util.List<Candle> candles, String dir) {
        StrategyVote vote = new StrategyVote();
        int last = candles.size() - 1;
        double rsi = calculateRSI(candles, last, 14);
        
        if (dir.equals("BUY") && rsi < 35) {
            vote.confidence = (35 - rsi) * 4; // RSI 10 = 100% confidence
            vote.direction = dir;
        } else if (dir.equals("SELL") && rsi > 65) {
            vote.confidence = (rsi - 65) * 4;
            vote.direction = dir;
        }
        vote.confidence = Math.min(100, vote.confidence);
        return vote;
    }

    private static double calculateMTFAlignment(String symbol, String baseTf) {
        String[] timeframes = {"D1", "H4", "H1"};
        double buyWeight = 0, sellWeight = 0;
        for (String tf : timeframes) {
            TrendStructure ts = getHigherTimeframeTrendStructure(symbol, tf, 300);
            if (ts.trend.equals("UP")) buyWeight += 1.0;
            else if (ts.trend.equals("DOWN")) sellWeight += 1.0;
        }
        // Agreement score (0.0 to 1.0)
        return Math.max(buyWeight, sellWeight) / 3.0;
    }

    private static java.util.List<TradeSignal> generateEliteQuantumSignals(java.util.List<Candle> candles, String symbol, String liveTimeframe) {
        java.util.List<TradeSignal> signals = new ArrayList<>();
        
        // --- 1. STRICT METAL RESTRICTION ---
        if (!symbol.equalsIgnoreCase("XAUUSD") && !symbol.equalsIgnoreCase("XAGUSD")) {
            return signals;
        }

        if (candles == null || candles.size() < 200) {
            return signals;
        }

        // --- 2. INSTITUTIONAL NEWS FILTER ---
        if (isHighImpactNewsWindow()) {
            System.out.println("🛡️ [Risk Protocol] News window active - suppressing Gold sniper for " + symbol);
            return signals;
        }

        int last = candles.size() - 1;
        Candle current = candles.get(last);
        double price = current.close;
        double atr = calculateATR(candles, last, 14);
        double atrPct = atr / Math.max(price, 1e-6);

        // --- 3. ELITE SPREAD FILTER ---
        double currentSpread = getCurrentSpreadPips(symbol);
        double maxSpread = symbol.equalsIgnoreCase("XAUUSD") ? 35.0 : 5.0;
        if (currentSpread > maxSpread) { 
            System.out.printf("⏩ [Skip %s] %s: Institutional spread too wide (%.2f)\n", 
                symbol.equalsIgnoreCase("XAUUSD") ? "Gold" : "Silver", symbol, currentSpread);
            return signals;
        }

        // --- 4. GLOBAL MACRO BIAS AUDIT (D1 + H4) ---
        // We require D1 trend for directional bias and H4 for structural confluence
        TrendStructure h4 = getHigherTimeframeTrendStructure(symbol, "H4", 450);
        TrendStructure h1 = getHigherTimeframeTrendStructure(symbol, "H1", 300);
        if (h4.trend.equals("FLAT")) return signals;

        // --- 5. INSTITUTIONAL TIMING (KILLZONES ONLY) ---
        if (!isWithinKillzone() && !parseBooleanEnv(LIVE_FORCE_SIGNAL_ENV)) {
            return signals; // Gold only moves with bank volume during London/NY open
        }

        // --- 6. LIQUIDITY HUNT MECHANISM (THE CORE) ---
        int sweepSignal = detectLiquiditySweepDirection(candles, last);
        boolean sweepBuy = sweepSignal == 1;
        boolean sweepSell = sweepSignal == -1;
        
        // Requirement: Must have a liquidity purge before sniper entry
        if (sweepSignal == 0 && !parseBooleanEnv(LIVE_FORCE_SIGNAL_ENV)) {
             return signals; 
        }

        // --- 7. DISPLACEMENT & MARKET STRUCTURE SHIFT (CHoCH) ---
        int choch = detectCHoCH(candles, last);
        int bos = detectBOS(candles, last);
        double displacement = calculateVolumeImbalance(candles, last);
        boolean displaced = Math.abs(displacement) > 0.7; // Large move after sweep
        
        // Fair Value Gap (Institutional Imbalance)
        double fvg = detectFairValueGap(candles, last);
        boolean fvgExists = Math.abs(fvg) > 0.2;

        // Order Block Detection (Institutional Support/Resistance)
        double orderBlock = detectOrderBlock(candles, last, 15);
        boolean obValid = orderBlock > 0.5;

        double orderFlow = calculateOrderFlowIntensity(candles, last);
        double vwap = calculateVWAP(candles, last, 20);

        int finalScore = 0;
        String direction = "NONE";
        String regime = "RANGING";

        // --- 8. QUANTUM SCORING SYSTEM (GOLD SPECIALIZED) ---
        int buyScore = 0;
        int sellScore = 0;
        StringBuilder audit = new StringBuilder("💰 [GOLD HUNT Audit] ");

        // Macro Alignment (35 pts) - Strict directional filtering
        if (h4.trend.equals("UP")) { buyScore += 35; audit.append("H4:UP "); } 
        else if (h4.trend.equals("DOWN")) { sellScore += 35; audit.append("H4:DOWN "); }
        
        if (h1.trend.equals("UP")) { buyScore += 10; audit.append("H1:UP "); } 
        else if (h1.trend.equals("DOWN")) { sellScore += 10; audit.append("H1:DOWN "); }

        // Liquidity & Displacement (45 pts)
        if (sweepBuy) { buyScore += 25; audit.append("SWEEP:BUY "); }
        if (sweepSell) { sellScore += 25; audit.append("SWEEP:SELL "); }
        
        if (displaced) {
            if (displacement > 0) { buyScore += 20; audit.append("DISP:UP "); } 
            else { sellScore += 20; audit.append("DISP:DOWN "); }
        }

        // Market Structure & Institutional Imbalance (25 pts)
        if (choch == 1 || bos == 1) { buyScore += 20; audit.append("STRUC:UP "); }
        if (choch == -1 || bos == -1) { sellScore += 20; audit.append("STRUC:DOWN "); }

        if (obValid) {
            if (current.close > current.open) buyScore += 10; else sellScore += 10;
            audit.append("OB:VALID ");
        }
        
        if (fvgExists) {
            if (fvg > 0) buyScore += 5; else sellScore += 5;
            audit.append("FVG:PASS ");
        }

        // --- UPGRADE 1: DYNAMIC ADAPTIVE THRESHOLD ---
        // Volatility-adjusted required score (higher ATR requires higher confluence)
        int minRequiredScore = (atrPct > 0.0012) ? 100 : 92;
        
        finalScore = Math.max(buyScore, sellScore);
        direction = (buyScore >= minRequiredScore) ? "BUY" : (sellScore >= minRequiredScore) ? "SELL" : "NONE";

        if (direction.equals("NONE")) {
            if (finalScore > 65) System.out.println(audit.toString() + " -> " + finalScore + "/" + minRequiredScore + " (Req: 92+)");
            return signals;
        }

        // Value Confirmation (10 pts)
        if (price > vwap && orderFlow > 0.55) buyScore += 10;
        if (price < vwap && orderFlow < 0.45) sellScore += 10;

        // Session & Regime Confluence
        regime = detectMarketRegime(candles, last, 30);
        if (regime.equals("TRENDING")) { buyScore += 10; sellScore += 10; }
        if (isWithinKillzone()) { buyScore += 15; sellScore += 15; }

        finalScore = Math.max(buyScore, sellScore);
        direction = (buyScore >= 95) ? "BUY" : (sellScore >= 95) ? "SELL" : "NONE";

        if (direction.equals("NONE")) {
            if (finalScore > 70) System.out.println(audit.toString() + " Confluence: " + finalScore + "/95 required for GOLD Sniper.");
            return signals;
        }

        // --- 9. RISK MANAGEMENT & EXECUTION ---
        double strength = Math.min(100.0, (double)finalScore);
        double mlProb = tradeDatabase.getHistoricalWinRate("GOLD_QILH_" + direction);
        
        // Stop Loss below/above the Liquidity Sweep Low/High
        double sl = Math.max(25.0, convertPriceDiffToPips(symbol, direction.equals("BUY") ? (price - getRecentLow(candles, last - 10, last)) : (getRecentHigh(candles, last - 10, last) - price)));
        sl = Math.min(sl, 60.0); // Never risk more than 60 pips on Gold

        // TP based on structural liquidity targets
        double tp = computeStructureBasedTP(candles, last, direction, price, sl, symbol);

        TradeSignal goldSignal = createEliteSignal(symbol, direction, price, sl, tp, mlProb, 0.95, strength, "AI QUANTUM SNIPER (GOLD)", direction.equals("BUY"));
        signals.add(goldSignal);

        System.out.println("🔥 [INSTITUTIONAL STRIKE] Gold Liquidity Hunt Executed: " + direction + " | Score: " + finalScore);

        updateGlobalIntelligence(symbol, candles);
        return signals;
    }
    private static double getCurrentSpreadPips(String symbol) {
        String env = System.getenv("CURRENT_SPREAD_" + symbol.toUpperCase());
        if (env != null && !env.isEmpty()) {
            try {
                return Double.parseDouble(env);
            } catch (Exception e) {
            }
        }

        // Dynamic Standard Spreads
        switch (symbol.toUpperCase()) {
            case "EURUSD":
                return 0.8;
            case "GBPUSD":
                return 1.2;
            case "USDJPY":
                return 1.0;
            case "XAUUSD":
                return 15.0; // Points
            case "XAGUSD":
                return 2.0; // Points
            case "BTCUSD":
                return 50.0; // Points ($0.50 approx)
            case "GBPJPY":
                return 2.2;
            default:
                return 1.5;
        }
    }

    // UPGRADE 3: Structure-based TP calculation
    // Scans next 30 candles for swing levels instead of using fixed 5x multiplier
    private static double computeStructureBasedTP(java.util.List<Candle> candles, int index, String direction, double entry, double slPips, String symbol) {
        if (index >= candles.size() - 1) {
            return slPips * 3.0; // fallback to 3:1 if insufficient candles
        }

        int lookAhead = Math.min(30, candles.size() - index - 1);
        double minDistancePips = slPips * 1.5; // TP must be at least 1.5x SL away
        double maxSearchPips = slPips * 3.0; // default fallback

        if ("BUY".equals(direction)) {
            // Find the nearest swing high above entry within next 30 bars
            double nearestSwingHigh = Double.MAX_VALUE;
            for (int i = index + 1; i < index + 1 + lookAhead; i++) {
                if (i < candles.size()) {
                    double swingHighPips = convertPriceDiffToPips(symbol, candles.get(i).high - entry);
                    if (swingHighPips >= minDistancePips && swingHighPips < nearestSwingHigh) {
                        nearestSwingHigh = swingHighPips;
                    }
                }
            }
            if (nearestSwingHigh < Double.MAX_VALUE && nearestSwingHigh <= slPips * 8.0) {
                System.out.printf("   [STRUCT] BUY TP anchored to swing high: %.1f pips (1:%.2f R:R)\n", nearestSwingHigh, nearestSwingHigh / slPips);
                return nearestSwingHigh;
            }
        } else {
            // Find the nearest swing low below entry within next 30 bars
            double nearestSwingLow = Double.MIN_VALUE;
            for (int i = index + 1; i < index + 1 + lookAhead; i++) {
                if (i < candles.size()) {
                    double swingLowPips = convertPriceDiffToPips(symbol, entry - candles.get(i).low);
                    if (swingLowPips >= minDistancePips && swingLowPips > nearestSwingLow) {
                        nearestSwingLow = swingLowPips;
                    }
                }
            }
            if (nearestSwingLow > Double.MIN_VALUE && nearestSwingLow <= slPips * 8.0) {
                System.out.printf("   [STRUCT] SELL TP anchored to swing low: %.1f pips (1:%.2f R:R)\n", nearestSwingLow, nearestSwingLow / slPips);
                return nearestSwingLow;
            }
        }

        // No valid structural level found; fall back to conservative 3:1 (not aggressive 5:1)
        System.out.printf("   [STRUCT] No swing level found; using conservative 3:1 TP\n");
        return slPips * 3.0;
    }

    private static TradeSignal createEliteSignal(String symbol, String direction, double entry, double slPips, double tpPips, double ml, double smc, double strength, String type, boolean bullish) {
        double slPrice = bullish ? (entry - convertPipsToPrice(symbol, slPips)) : (entry + convertPipsToPrice(symbol, slPips));
        double tpPrice = bullish ? (entry + convertPipsToPrice(symbol, tpPips)) : (entry - convertPipsToPrice(symbol, tpPips));

        String reason = String.format("ELITE %s: Confluence %.2f | RR 1:%.1f", type, smc, tpPips / slPips);
        TradeSignal signal = new TradeSignal(symbol, direction, entry, slPrice, tpPrice, ml, smc, strength, reason, slPips, tpPips);
        signal.setupType = type;
        signal.precisionScore = (int) strength;
        return signal;
    }

    private static boolean detectLiquiditySweep(java.util.List<Candle> candles, int index) {
        return detectLiquiditySweepDirection(candles, index) != 0;
    }

    private static int detectLiquiditySweepDirection(java.util.List<Candle> candles, int index) {
        if (index < 30) {
            return 0;
        }
        double atr = calculateATR(candles, index, 14);
        double recentHigh = getRecentHigh(candles, index - 20, index - 1);
        double recentLow = getRecentLow(candles, index - 20, index - 1);
        Candle current = candles.get(index);
        double avgVolume = calculateAverageVolume(candles, index - 1, 20);

        boolean volumeSpike = current.volume > avgVolume * 1.5;

        if (current.low < recentLow && current.close > recentLow) {
            double sweepDistance = recentLow - current.low;
            if (sweepDistance > atr * 0.25 && volumeSpike) {
                return 1;
            }
        }
        if (current.high > recentHigh && current.close < recentHigh) {
            double sweepDistance = current.high - recentHigh;
            if (sweepDistance > atr * 0.25 && volumeSpike) {
                return -1;
            }
        }
        return 0;
    }

    private static java.util.List<TradeSignal> generateLiveSignalsForStrategy(java.util.List<Candle> candles, String symbol, String liveTimeframe, String strategy) {
        if (candles == null || candles.isEmpty()) {
            return new ArrayList<>();
        }

        String mode = strategy == null ? DEFAULT_LIVE_STRATEGY_MODE : strategy.toLowerCase(Locale.ROOT).trim();
        
        // Master QFE Integration
        if (mode.equals("auto") || mode.equals("fusion") || mode.equals("qfe")) {
            return generateQuantumFusionSignals(candles, symbol, liveTimeframe);
        }

        switch (mode) {
            case "quantum":
                return generateEliteQuantumSignals(candles, symbol, liveTimeframe);
            case "sniper":
                return generateSniperSignals(candles, symbol, liveTimeframe);
            case "meanreversion":
                return generateMeanReversionSignals(candles, symbol, liveTimeframe);
            case "breakout":
                return generateBreakoutSignals(candles, symbol, liveTimeframe);
            case "smc":
                return generateSMCSignals(candles, symbol, liveTimeframe);
            case "all":
                java.util.List<TradeSignal> all = new ArrayList<>();
                all.addAll(generateEliteQuantumSignals(candles, symbol, liveTimeframe));
                all.addAll(generateSMCSignals(candles, symbol, liveTimeframe));
                all.addAll(generateSniperSignals(candles, symbol, liveTimeframe));
                all.addAll(generateBreakoutSignals(candles, symbol, liveTimeframe));
                all.addAll(generateMeanReversionSignals(candles, symbol, liveTimeframe));
                return all;
            default:
                return generateSMCSignals(candles, symbol, liveTimeframe);
        }
    }

    private static java.util.List<TradeSignal> generateSMCSignals(java.util.List<Candle> candles, String symbol, String liveTimeframe) {
        java.util.List<TradeSignal> signals = new ArrayList<>();
        if (isHighImpactNewsWindow()) {
            return signals;
        }
        if (candles == null || candles.size() < TREND_EMA_LONG + 5) {
            return signals;
        }

        int lastIndex = candles.size() - 1;
        double currentPrice = candles.get(lastIndex).close;
        double longEma = calculateEMA(candles, lastIndex, TREND_EMA_LONG);
        double shortEma = calculateEMA(candles, lastIndex, TREND_EMA_SHORT);
        double currentRsi = calculateRSI(candles, lastIndex, TREND_RSI_PERIOD);
        double atr = calculateATR(candles, lastIndex, 14);
        double atrPct = atr / Math.max(currentPrice, 1e-6);
        int structure = detectMarketStructure(candles, lastIndex, 20);
        double orderBlock = detectOrderBlock(candles, lastIndex, 20);
        double liquidity = detectLiquidityZone(candles, lastIndex, 20);
        double fvg = detectFairValueGap(candles, lastIndex);
        double levelFlip = detectLevelFlip(candles, lastIndex, 20);
        double momentum = (shortEma - longEma) / Math.max(longEma, 1e-6);
        boolean forceSignal = parseBooleanEnv(LIVE_FORCE_SIGNAL_ENV);

        double stopLossDistance = Math.max(atr * 1.5, convertPipsToPrice(symbol, 15.0));
        double takeProfitMultiplier = 2.5;
        String regime = detectMarketRegime(candles, lastIndex, 30);
        double strength = 85.0;

        if (atrPct > 0 && atrPct < MIN_ATR_PERCENT && !forceSignal) {
            System.out.printf("   ❌ HOLD: volatility too low for %s on %s: ATRpct=%.6f < %.6f%n", symbol, liveTimeframe, atrPct, MIN_ATR_PERCENT);
            return signals;
        }

        boolean masterTrigger = isMasterTrendTrigger(currentPrice, shortEma, longEma, structure);
        if (!masterTrigger && !forceSignal) {
            System.out.printf("   ❌ HOLD: master trend trigger not confirmed for %s on %s.%n", symbol, liveTimeframe);
            return signals;
        }

        double liquidityConfirmation = calculateLiquidityConfirmation(orderBlock, liquidity);
        if (liquidityConfirmation < 0.60 && !forceSignal) {
            System.out.printf("   ❌ HOLD: liquidity confirmation too weak for %s on %s: %.2f\n", symbol, liveTimeframe, liquidityConfirmation);
            return signals;
        }

        boolean buySetup = structure == 1 && currentPrice > shortEma;
        boolean sellSetup = structure == -1 && currentPrice < shortEma;
        
        // UPGRADE 3: Directional ML confidence check
        double mlConfidence = estimateLiveMlProbability(momentum, orderBlock, liquidity, currentRsi, fvg, levelFlip, structure, buySetup);
        if (mlConfidence < 0.85 && !forceSignal) {
            System.out.printf("   ❌ HOLD: ML confirmation below 85%% for %s on %s: %.2f\n", symbol, liveTimeframe, mlConfidence);
            return signals;
        }

        if ((buySetup || sellSetup) || forceSignal) {
            double entry = currentPrice;
            double stopLoss = buySetup ? entry - stopLossDistance : entry + stopLossDistance;
            double takeProfit = buySetup ? entry + stopLossDistance * takeProfitMultiplier : entry - stopLossDistance * takeProfitMultiplier;
            String reason = String.format("A+ SMC %s: structure=%d, liquidity=%.2f, ML=%.2f, ATRpct=%.6f, regime=%s",
                    buySetup ? "BUY" : "SELL", structure, liquidity, mlConfidence, atrPct, regime);
            if (buySetup) {
                signals.add(new TradeSignal(symbol, "BUY", entry, stopLoss, takeProfit, mlConfidence, liquidityConfirmation, strength, reason, stopLossDistance, stopLossDistance * takeProfitMultiplier));
            } else if (sellSetup) {
                signals.add(new TradeSignal(symbol, "SELL", entry, stopLoss, takeProfit, mlConfidence, liquidityConfirmation, strength, reason, stopLossDistance, stopLossDistance * takeProfitMultiplier));
            }
        }

        if (signals.isEmpty()) {
            String holdReason = "no A+ master signal confirmed";
            System.out.println("   FINAL DECISION: HOLD - " + holdReason + ".");
        } else {
            for (TradeSignal signal : signals) {
                System.out.println("   FINAL SIGNAL: " + signal.direction + " | " + signal.reason);
            }
        }

        return signals;
    }

    private static double calculateLiquidityConfirmation(double orderBlock, double liquidity) {
        return Math.min(1.0, 0.40 * Math.min(1.0, orderBlock / 0.40) + 0.60 * Math.min(1.0, liquidity / 0.55));
    }

    private static boolean isMasterTrendTrigger(double currentPrice, double shortEma, double longEma, int structure) {
        if (structure == 1) {
            return currentPrice > longEma && shortEma > longEma;
        }
        if (structure == -1) {
            return currentPrice < longEma && shortEma < longEma;
        }
        return false;
    }

    private static double estimateLiveMlProbability(double momentum, double orderBlock, double liquidity, double currentRsi, double fvg, double levelFlip, int structure, boolean buySide) {
        double score = 0.42; // Base institutional floor
        
        // Alignment checks
        boolean momentumAlign = (buySide && momentum > 0) || (!buySide && momentum < 0);
        boolean structureAlign = (buySide && structure == 1) || (!buySide && structure == -1);
        
        score += momentumAlign ? Math.min(0.22, Math.abs(momentum) * 75.0) : -0.10;
        score += Math.min(0.18, orderBlock * 0.18);
        score += Math.min(0.15, liquidity * 0.15);
        
        // RSI efficiency (Institutional range 40-60 is neutral, outside is expansion)
        if (buySide && currentRsi > 45 && currentRsi < 75) score += 0.10;
        else if (!buySide && currentRsi < 55 && currentRsi > 25) score += 0.10;
        
        score += Math.min(0.12, Math.abs(fvg) * 0.12);
        score += structureAlign ? 0.12 : -0.05;
        score += Math.min(0.08, Math.abs(levelFlip) * 0.08);
        
        return Math.min(1.0, Math.max(0.0, score));
    }

    private static double calculateLiveTakeProfitMultiplier(double orderBlock, double fvg, double atrPct) {
        double multiplier = 2.0;
        multiplier += Math.min(0.30, orderBlock * 0.30);
        multiplier += fvg > 0.25 ? 0.15 : 0.0;
        multiplier += atrPct > 0.0009 ? 0.15 : 0.0;
        multiplier = Math.min(3.2, multiplier);
        return Math.max(2.0, multiplier);
    }

    private static double calculateAPlusSignalStrength(double orderBlock, double liquidity, double momentum, double currentRsi, double mlConfidence, double liquidityConfirmation, int structure) {
        double strength = 30.0;
        strength += Math.min(1.0, orderBlock) * 22.0;
        strength += Math.min(1.0, liquidity) * 22.0;
        strength += Math.min(1.0, Math.abs(momentum) * 1.5) * 18.0;
        strength += mlConfidence * 15.0;
        strength += liquidityConfirmation * 10.0;
        strength += structure != 0 ? 5.0 : 0.0;
        strength += (currentRsi > 35 && currentRsi < 65) ? 5.0 : 0.0;
        return Math.min(100.0, Math.max(0.0, strength));
    }

    private static double calculateSMCSetupScore(double orderBlock, double liquidity, double momentum, double currentRsi, double fvg, double levelFlip, int structure, TrendStructure htf, String regime) {
        double score = 0.0;
        score += Math.min(1.0, orderBlock) * 0.28;
        score += Math.min(1.0, liquidity) * 0.18;
        score += Math.min(1.0, Math.abs(momentum) * 1.5) * 0.16;
        score += (structure == 1 || structure == -1 ? 0.12 : 0.0);
        score += ("UP".equals(htf.trend) && structure == 1) || ("DOWN".equals(htf.trend) && structure == -1) ? 0.10 : 0.0;
        score += fvg > 0.35 ? 0.05 : 0.0;
        score += Math.abs(levelFlip) > 0.35 ? 0.04 : 0.0;
        score += (currentRsi > 30 && currentRsi < 70) ? 0.05 : -0.04;
        score += "trending".equals(regime) ? 0.05 : 0.0;
        score += "volatile".equals(regime) ? 0.03 : 0.0;
        return Math.min(1.0, Math.max(0.0, score));
    }

    private static double calculateSMCStopLossDistance(double atr, double minPriceDistance, double liquidity, double atrPct) {
        double volatilityFactor = 1.0 + Math.max(0.0, 0.18 - liquidity * 0.13);
        double atrMultiplier = getDynamicAtrMultiplier(atrPct);
        return Math.max(atr * atrMultiplier, minPriceDistance) * volatilityFactor;
    }

    private static double getDynamicAtrMultiplier(double atrPct) {
        if (atrPct >= 0.0009) {
            return 2.2;
        }
        if (atrPct <= 0.0002) {
            return 1.2;
        }
        return 1.4;
    }

    private static double calculateSMCTakeProfitMultiplier(double orderBlock, double fvg, String regime) {
        double multiplier = 2.2;
        multiplier += Math.min(0.35, orderBlock * 0.35);
        multiplier += fvg > 0.40 ? 0.20 : 0.0;
        multiplier += "trending".equals(regime) ? 0.12 : 0.0;
        multiplier += "volatile".equals(regime) ? 0.08 : 0.0;
        return Math.min(3.5, Math.max(2.0, multiplier));
    }

    private static double calculateSMCSignalStrength(double setupScore, double smcConfidence, double momentum, double currentRsi) {
        double strength = 30.0;
        strength += setupScore * 30.0;
        strength += smcConfidence * 25.0;
        strength += Math.min(20.0, Math.abs(momentum) * 80.0);
        strength += (currentRsi > 40 && currentRsi < 60) ? 5.0 : 0.0;
        return Math.min(100.0, Math.max(0.0, strength));
    }

    private static void displayStrategySignals(String strategy, java.util.List<TradeSignal> signals) {
        System.out.println();
        System.out.println("════════════════════════════════════════");
        System.out.println("📌 Strategy: " + strategy.toUpperCase() + " signals");
        System.out.println("════════════════════════════════════════");
        if (signals == null || signals.isEmpty()) {
            System.out.println("   No signals generated by " + strategy + ".");
            return;
        }
        signals.sort((a, b) -> Double.compare(b.signalStrength, a.signalStrength));
        for (TradeSignal signal : signals) {
            System.out.printf("   [%s] %s %s entry=%.5f SL=%.5f TP=%.5f | RR=%.2f | strength=%.1f%% | reward=%.2f | reason=%s%n",
                    strategy, signal.symbol, signal.direction, signal.entry, signal.stopLoss, signal.takeProfit,
                    signal.riskRewardRatio, signal.signalStrength, signal.rewardAmount, signal.reason);
        }
        TradeSignal top = getMaxWinningsSignal(signals);
        if (top != null) {
            System.out.printf("   ▶ Top %s candidate: %s %s entry=%.5f TP=%.5f reward=%.2f | strength=%.1f%%%n",
                    strategy, top.symbol, top.direction, top.entry, top.takeProfit, top.rewardAmount, top.signalStrength);
        }
    }

    private static TradeSignal getMaxWinningsSignal(java.util.List<TradeSignal> signals) {
        TradeSignal best = null;
        if (signals == null) {
            return null;
        }
        for (TradeSignal signal : signals) {
            if (best == null || signal.rewardAmount > best.rewardAmount || (signal.rewardAmount == best.rewardAmount && signal.signalStrength > best.signalStrength)) {
                best = signal;
            }
        }
        return best;
    }

    private static java.util.List<TradeSignal> generateMeanReversionSignals(java.util.List<Candle> candles, String symbol, String liveTimeframe) {
        java.util.List<TradeSignal> signals = new ArrayList<>();
        if (isHighImpactNewsWindow()) {
            return signals;
        }
        if (candles == null || candles.size() < TREND_EMA_LONG + 5) {
            return signals;
        }

        int lastIndex = candles.size() - 1;
        double currentPrice = candles.get(lastIndex).close;
        double longEma = calculateEMA(candles, lastIndex, TREND_EMA_LONG);
        double shortEma = calculateEMA(candles, lastIndex, TREND_EMA_SHORT);
        double ema20 = calculateEMA(candles, lastIndex, 20);
        double currentRsi = calculateRSI(candles, lastIndex, TREND_RSI_PERIOD);
        double momentum = (shortEma - longEma) / longEma;
        double atr = calculateATR(candles, lastIndex, 14);
        double atrPct = atr / Math.max(currentPrice, 1e-6);
        int structure = detectMarketStructure(candles, lastIndex, 20);

        if (Math.abs(momentum) < MIN_SIGNAL_MOMENTUM) {
            System.out.printf("   ❌ HOLD: mean reversal momentum too weak for %s on %s: %.4f < %.4f\n", symbol, liveTimeframe, momentum, MIN_SIGNAL_MOMENTUM);
            return signals;
        }
        if (Math.abs(momentum) >= MAX_MEAN_REVERSION_MOMENTUM) {
            System.out.printf("   ❌ HOLD: mean reversal momentum too strong for %s on %s: %.4f >= %.4f\n", symbol, liveTimeframe, Math.abs(momentum), MAX_MEAN_REVERSION_MOMENTUM);
            return signals;
        }
        if (atrPct > 0 && atrPct < MIN_ATR_PERCENT) {
            System.out.printf("   ❌ HOLD: mean reversal volatility too low for %s on %s: ATRpct=%.6f < %.6f\n", symbol, liveTimeframe, atrPct, MIN_ATR_PERCENT);
            return signals;
        }
        boolean forceSignal = parseBooleanEnv(LIVE_FORCE_SIGNAL_ENV);
        if (structure == 0 && !forceSignal) {
            System.out.printf("   ❌ HOLD: mean reversal structure unclear for %s on %s.\n", symbol, liveTimeframe);
            return signals;
        }

        String htfTimeframe = System.getenv(LIVE_HTF_TIMEFRAME_ENV);
        if (htfTimeframe == null || htfTimeframe.isEmpty()) {
            htfTimeframe = getHigherTimeframeFor(liveTimeframe);
        }
        int htfCount = Math.max(220, TREND_EMA_LONG + 5);
        String htfCountEnv = System.getenv(LIVE_HTF_CANDLES_COUNT_ENV);
        if (htfCountEnv != null && !htfCountEnv.isEmpty()) {
            try {
                htfCount = Math.max(TREND_EMA_LONG + 5, Integer.parseInt(htfCountEnv.trim()));
            } catch (NumberFormatException ignored) {
            }
        }
        boolean skipHtfStructure = parseBooleanEnv(LIVE_SKIP_HTF_STRUCTURE_ENV);
        TrendStructure htf = getHigherTimeframeTrendStructure(symbol, htfTimeframe, htfCount);
        if (htf.structure == 0 && !skipHtfStructure && !forceSignal) {
            System.out.println("   ❌ HOLD: mean reversal HTF structure unclear for " + symbol + " on " + htfTimeframe + ".");
            return signals;
        }
        if (skipHtfStructure && htf.structure == 0) {
            System.out.println("   ⚠️ Skipping mean reversal HTF structure hold for " + symbol + " on " + htfTimeframe + " due to LIVE_SKIP_HTF_STRUCTURE=true.");
        }
        if (forceSignal && htf.structure == 0) {
            System.out.println("   ⚠️ Forcing mean reversal signal for " + symbol + " on " + htfTimeframe + " due to LIVE_FORCE_SIGNAL=true.");
        }

        double recentLow = getRecentLow(candles, Math.max(0, lastIndex - 20), lastIndex - 1);
        double recentHigh = getRecentHigh(candles, Math.max(0, lastIndex - 20), lastIndex - 1);
        double oversoldPriceDistance = (ema20 - currentPrice) / Math.max(ema20, 1e-6);
        double overboughtPriceDistance = (currentPrice - ema20) / Math.max(ema20, 1e-6);
        forceSignal = parseBooleanEnv(LIVE_FORCE_SIGNAL_ENV);
        boolean buyZone = currentRsi <= MEAN_REVERSION_OVERSOLD && currentPrice < ema20 && currentPrice <= recentLow + atr * 0.5 && (structure == -1 || forceSignal) && !"DOWN".equals(htf.trend);
        boolean sellZone = currentRsi >= MEAN_REVERSION_OVERBOUGHT && currentPrice > ema20 && currentPrice >= recentHigh - atr * 0.5 && (structure == 1 || forceSignal) && !"UP".equals(htf.trend);

        if (buyZone) {
            double entry = currentPrice;
            double stopLossDistance = Math.max(convertPipsToPrice(symbol, getPairStopLossPips(symbol)), atr * 1.5);
            double stopLoss = entry - stopLossDistance;
            double takeProfit = entry + stopLossDistance * 2.0;
            double strength = Math.min(100.0, 50.0 + (MEAN_REVERSION_OVERSOLD - currentRsi) * 1.5 + oversoldPriceDistance * 100.0);
            String reason = String.format("Mean Reversion BUY: oversold RSI=%.2f, local low support, HTF=%s", currentRsi, htf.trend);
            signals.add(new TradeSignal(symbol, "BUY", entry, stopLoss, takeProfit, 0.70, 0.70, strength, reason, atr, atr * 2.0));
        } else if (sellZone) {
            double entry = currentPrice;
            double stopLossDistance = Math.max(convertPipsToPrice(symbol, getPairStopLossPips(symbol)), atr * 1.5);
            double stopLoss = entry + stopLossDistance;
            double takeProfit = entry - stopLossDistance * 2.0;
            double strength = Math.min(100.0, 50.0 + (currentRsi - MEAN_REVERSION_OVERBOUGHT) * 1.5 + overboughtPriceDistance * 100.0);
            String reason = String.format("Mean Reversion SELL: overbought RSI=%.2f, local high resistance, HTF=%s", currentRsi, htf.trend);
            signals.add(new TradeSignal(symbol, "SELL", entry, stopLoss, takeProfit, 0.70, 0.70, strength, reason, atr, atr * 2.0));
        } else {
            java.util.List<String> holdReasons = new ArrayList<>();
            if (!"DOWN".equals(htf.trend) && !"UP".equals(htf.trend)) {
                holdReasons.add("HTF trend neutral or flat");
            }
            if (currentRsi > MEAN_REVERSION_OVERSOLD && currentRsi < MEAN_REVERSION_OVERBOUGHT) {
                holdReasons.add("RSI not extreme enough");
            }
            if (Math.abs(momentum) < MIN_SIGNAL_MOMENTUM) {
                holdReasons.add("momentum too weak");
            }
            if (Math.abs(momentum) >= MAX_MEAN_REVERSION_MOMENTUM) {
                holdReasons.add("momentum too strong for mean reversion");
            }
            if (structure == 1 && currentRsi <= MEAN_REVERSION_OVERSOLD) {
                holdReasons.add("bullish structure conflicts with oversold signal");
            }
            if (structure == -1 && currentRsi >= MEAN_REVERSION_OVERBOUGHT) {
                holdReasons.add("bearish structure conflicts with overbought signal");
            }
            if (holdReasons.isEmpty()) {
                holdReasons.add("not a strong mean reversal setup");
            }
            System.out.println("   FINAL DECISION: HOLD - " + String.join("; ", holdReasons) + ".");
        }

        if (!signals.isEmpty()) {
            for (TradeSignal signal : signals) {
                System.out.println("   FINAL SIGNAL: " + signal.direction + " | " + signal.reason);
            }
        }

        return signals;
    }

    private static java.util.List<TradeSignal> generateBreakoutSignals(java.util.List<Candle> candles, String symbol, String liveTimeframe) {
        java.util.List<TradeSignal> signals = new ArrayList<>();
        if (isHighImpactNewsWindow()) {
            return signals;
        }
        if (candles == null || candles.size() < TREND_EMA_LONG + 5) {
            return signals;
        }

        int lastIndex = candles.size() - 1;
        double currentPrice = candles.get(lastIndex).close;
        double longEma = calculateEMA(candles, lastIndex, TREND_EMA_LONG);
        double shortEma = calculateEMA(candles, lastIndex, TREND_EMA_SHORT);
        double currentRsi = calculateRSI(candles, lastIndex, TREND_RSI_PERIOD);
        double momentum = (shortEma - longEma) / longEma;
        double atr = calculateATR(candles, lastIndex, 14);
        double atrPct = atr / Math.max(currentPrice, 1e-6);
        int structure = detectMarketStructure(candles, lastIndex, 20);
        double recentHigh = getRecentHigh(candles, Math.max(0, lastIndex - 16), lastIndex - 1);
        double recentLow = getRecentLow(candles, Math.max(0, lastIndex - 16), lastIndex - 1);
        double rangePct = Math.abs(recentHigh - recentLow) / Math.max(currentPrice, 1e-6);

        if (atrPct > 0 && atrPct < MIN_ATR_PERCENT) {
            System.out.printf("   ❌ HOLD: breakout volatility too low for %s on %s: ATRpct=%.6f < %.6f\n", symbol, liveTimeframe, atrPct, MIN_ATR_PERCENT);
            return signals;
        }
        boolean forceSignal = parseBooleanEnv(LIVE_FORCE_SIGNAL_ENV);
        if (Math.abs(momentum) < MIN_SIGNAL_MOMENTUM) {
            System.out.printf("   ❌ HOLD: breakout momentum too weak for %s on %s: %.4f < %.4f\n", symbol, liveTimeframe, momentum, MIN_SIGNAL_MOMENTUM);
            return signals;
        }
        if (structure == 0 && !forceSignal) {
            System.out.printf("   ❌ HOLD: breakout structure unclear for %s on %s.\n", symbol, liveTimeframe);
            return signals;
        }
        boolean skipBreakoutRange = parseBooleanEnv(LIVE_SKIP_BREAKOUT_RANGE_ENV);
        if (rangePct > BREAKOUT_CONSOLIDATION_RANGE_PCT && !skipBreakoutRange) {
            System.out.printf("   ❌ HOLD: breakout range too wide for %s on %s: %.4f > %.4f\n", symbol, liveTimeframe, rangePct, BREAKOUT_CONSOLIDATION_RANGE_PCT);
            return signals;
        }
        if (skipBreakoutRange && rangePct > BREAKOUT_CONSOLIDATION_RANGE_PCT) {
            System.out.printf("   ⚠️ Skipping breakout range hold for %s on %s: %.4f > %.4f due to LIVE_SKIP_BREAKOUT_RANGE=true\n", symbol, liveTimeframe, rangePct, BREAKOUT_CONSOLIDATION_RANGE_PCT);
        }

        String htfTimeframe = System.getenv(LIVE_HTF_TIMEFRAME_ENV);
        if (htfTimeframe == null || htfTimeframe.isEmpty()) {
            htfTimeframe = getHigherTimeframeFor(liveTimeframe);
        }
        int htfCount = Math.max(220, TREND_EMA_LONG + 5);
        String htfCountEnv = System.getenv(LIVE_HTF_CANDLES_COUNT_ENV);
        if (htfCountEnv != null && !htfCountEnv.isEmpty()) {
            try {
                htfCount = Math.max(TREND_EMA_LONG + 5, Integer.parseInt(htfCountEnv.trim()));
            } catch (NumberFormatException ignored) {
            }
        }
        boolean skipHtfStructure = parseBooleanEnv(LIVE_SKIP_HTF_STRUCTURE_ENV);
        TrendStructure htf = getHigherTimeframeTrendStructure(symbol, htfTimeframe, htfCount);
        if (htf.structure == 0 && !skipHtfStructure && !forceSignal) {
            System.out.println("   ❌ HOLD: breakout HTF structure unclear for " + symbol + " on " + htfTimeframe + ".");
            return signals;
        }
        if (skipHtfStructure && htf.structure == 0) {
            System.out.println("   ⚠️ Skipping breakout HTF structure hold for " + symbol + " on " + htfTimeframe + " due to LIVE_SKIP_HTF_STRUCTURE=true.");
        }
        if (forceSignal && htf.structure == 0) {
            System.out.println("   ⚠️ Forcing breakout signal for " + symbol + " on " + htfTimeframe + " due to LIVE_FORCE_SIGNAL=true.");
        }

        boolean breakoutBuy = currentPrice > recentHigh + atr * BREAKOUT_ATR_MULTIPLIER && currentRsi >= BREAKOUT_RSI_MIN && momentum > 0 && (structure == 1 || forceSignal) && ("UP".equals(htf.trend) || "FLAT".equals(htf.trend) || forceSignal);
        boolean breakoutSell = currentPrice < recentLow - atr * BREAKOUT_ATR_MULTIPLIER && currentRsi <= 100.0 - BREAKOUT_RSI_MIN && momentum < 0 && (structure == -1 || forceSignal) && ("DOWN".equals(htf.trend) || "FLAT".equals(htf.trend) || forceSignal);

        if (breakoutBuy) {
            double entry = currentPrice;
            double stopLossDistance = Math.max(atr * 1.5, convertPipsToPrice(symbol, getPairStopLossPips(symbol)));
            double stopLoss = entry - stopLossDistance;
            double takeProfit = entry + stopLossDistance * 2.0;
            double strength = Math.min(100.0, 55.0 + (currentPrice - recentHigh) / Math.max(atr, 1e-6) * 20.0 + (currentRsi - BREAKOUT_RSI_MIN) * 0.8);
            String reason = String.format("Breakout BUY: cleared recent high, H4/H1=%s, RSI=%.2f", htf.trend, currentRsi);
            signals.add(new TradeSignal(symbol, "BUY", entry, stopLoss, takeProfit, 0.75, 0.75, strength, reason, stopLossDistance, stopLossDistance * 2.0));
        }
        if (breakoutSell) {
            double entry = currentPrice;
            double stopLoss = recentLow + atr * 0.1;
            double stopLossDistance = Math.max(atr * 1.5, convertPipsToPrice(symbol, getPairStopLossPips(symbol)));
            double takeProfit = entry - stopLossDistance * 2.0;
            double strength = Math.min(100.0, 55.0 + (recentLow - currentPrice) / Math.max(atr, 1e-6) * 20.0 + (BREAKOUT_RSI_MIN - currentRsi) * 0.8);
            String reason = String.format("Breakout SELL: broke below recent low, H4/H1=%s, RSI=%.2f", htf.trend, currentRsi);
            signals.add(new TradeSignal(symbol, "SELL", entry, stopLoss, takeProfit, 0.75, 0.75, strength, reason, atr, atr * 2.0));
        }

        if (signals.isEmpty()) {
            java.util.List<String> holdReasons = new ArrayList<>();
            if (currentRsi < BREAKOUT_RSI_MIN && currentRsi > 100.0 - BREAKOUT_RSI_MIN) {
                holdReasons.add("RSI not supportive for breakout");
            }
            if (Math.abs(momentum) < MIN_SIGNAL_MOMENTUM) {
                holdReasons.add("breakout momentum too weak");
            }
            if (rangePct > BREAKOUT_CONSOLIDATION_RANGE_PCT) {
                holdReasons.add("range too wide for focused breakout");
            }
            if (breakoutBuy || breakoutSell) {
                // no-op
            } else {
                holdReasons.add("no clean breakout level detected");
            }
            System.out.println("   FINAL DECISION: HOLD - " + String.join("; ", holdReasons) + ".");
        } else {
            for (TradeSignal signal : signals) {
                System.out.println("   FINAL SIGNAL: " + signal.direction + " | " + signal.reason);
            }
        }

        return signals;
    }

    private static double getRecentHigh(java.util.List<Candle> candles, int startIndex, int endIndex) {
        double high = Double.NEGATIVE_INFINITY;
        for (int i = Math.max(0, startIndex); i <= Math.min(endIndex, candles.size() - 1); i++) {
            high = Math.max(high, candles.get(i).high);
        }
        return high == Double.NEGATIVE_INFINITY ? 0.0 : high;
    }

    private static double getRecentLow(java.util.List<Candle> candles, int startIndex, int endIndex) {
        double low = Double.POSITIVE_INFINITY;
        for (int i = Math.max(0, startIndex); i <= Math.min(endIndex, candles.size() - 1); i++) {
            low = Math.min(low, candles.get(i).low);
        }
        return low == Double.POSITIVE_INFINITY ? 0.0 : low;
    }

    private static String getHigherTimeframeTrend(String symbol, String timeframe, int count) {
        java.util.List<Candle> candles = fetchMarketCandles(symbol, count, timeframe);
        if (candles == null || candles.size() < TREND_EMA_LONG + 5) {
            System.out.println("   ⚠️ HTF trend check failed: not enough " + timeframe + " candles for " + symbol + ".");
            return "UNKNOWN";
        }

        int lastIndex = candles.size() - 1;
        int prevIndex = lastIndex - 1;
        double currentPrice = candles.get(lastIndex).close;
        double longEma = calculateEMA(candles, lastIndex, TREND_EMA_LONG);
        double shortEma = calculateEMA(candles, lastIndex, TREND_EMA_SHORT);
        int structure = detectMarketStructure(candles, lastIndex, 20);

        boolean emaUp = currentPrice > longEma && shortEma > longEma;
        boolean emaDown = currentPrice < longEma && shortEma < longEma;
        boolean momentumUp = (shortEma - longEma) / longEma > 0.0005;
        boolean momentumDown = (longEma - shortEma) / longEma > 0.0005;
        boolean uptrend = emaUp && (structure == 1 || momentumUp);
        boolean downtrend = emaDown && (structure == -1 || momentumDown);

        System.out.printf("   📈 HTF %s trend for %s: price=%.5f, EMA50=%.5f, EMA200=%.5f, structure=%d, momentum=%.4f -> %s\n",
                timeframe, symbol, currentPrice, shortEma, longEma, structure, shortEma - longEma, uptrend ? "UP" : downtrend ? "DOWN" : "FLAT");

        if (uptrend) {
            return "UP";
        }
        if (downtrend) {
            return "DOWN";
        }
        return "FLAT";
    }

    private static String getHigherTimeframeFor(String timeframe) {
        if (timeframe == null || timeframe.isEmpty()) {
            return "H1";
        }
        timeframe = timeframe.toUpperCase().trim();
        switch (timeframe) {
            case "M1":
            case "M2":
                return "M5";
            case "M3":
            case "M4":
            case "M5":
                return "M15";
            case "M15":
                return "M30";
            case "M30":
                return "H1";
            case "H1":
                return "H4";
            case "H2":
            case "H4":
                return "D1";
            case "D1":
                return "W1";
            case "W1":
                return "MN1";
            case "MN1":
                return "MN3";
            default:
                return "H4";
        }
    }

    private static String getSymbolAwareHigherTimeframe(String timeframe, String symbol) {
        return getHigherTimeframeFor(timeframe);
    }

    private static String getSecondaryHigherTimeframeFor(String timeframe) {
        if (timeframe == null || timeframe.isEmpty()) {
            return "H1";
        }
        timeframe = timeframe.toUpperCase().trim();
        switch (timeframe) {
            case "M1":
            case "M2":
            case "M3":
            case "M4":
            case "M5":
                return "M30";
            case "M15":
                return "H1";
            case "M30":
                return "H1";
            case "H1":
            case "H2":
                return "H4";
            case "H4":
                return "D1";
            case "D1":
                return "H4";
            case "W1":
                return "D1";
            default:
                return null;
        }
    }

    private static String getSymbolAwareSecondaryHigherTimeframe(String timeframe, String symbol) {
        return getSecondaryHigherTimeframeFor(timeframe);
    }

    private static double[] getLiveRsiThresholds(String timeframe, String symbol) {
        if (timeframe == null || timeframe.isEmpty()) {
            timeframe = "H1";
        }
        timeframe = timeframe.toUpperCase().trim();
        boolean isGold = symbol != null && symbol.equalsIgnoreCase("XAUUSD");
        switch (timeframe) {
            case "M1":
                return isGold ? new double[]{28.0, 72.0, 24.0, 76.0} : new double[]{32.0, 68.0, 28.0, 72.0};
            case "M5":
                return isGold ? new double[]{30.0, 70.0, 26.0, 74.0} : new double[]{34.0, 66.0, 30.0, 70.0};
            case "M15":
                return isGold ? new double[]{32.0, 68.0, 28.0, 72.0} : new double[]{36.0, 64.0, 32.0, 68.0};
            case "M30":
                return isGold ? new double[]{34.0, 66.0, 30.0, 70.0} : new double[]{38.0, 62.0, 34.0, 66.0};
            case "H1":
                return isGold ? new double[]{36.0, 64.0, 32.0, 68.0} : new double[]{40.0, 60.0, 35.0, 65.0};
            case "H2":
                return isGold ? new double[]{37.0, 63.0, 33.0, 67.0} : new double[]{41.0, 59.0, 36.0, 64.0};
            case "H4":
                return isGold ? new double[]{38.0, 62.0, 34.0, 66.0} : new double[]{42.0, 58.0, 38.0, 62.0};
            case "D1":
                return isGold ? new double[]{40.0, 60.0, 36.0, 64.0} : new double[]{45.0, 55.0, 40.0, 60.0};
            default:
                return isGold ? new double[]{36.0, 64.0, 32.0, 68.0} : new double[]{40.0, 60.0, 35.0, 65.0};
        }
    }

    private static boolean isLiveNewsTradeMode() {
        return parseBooleanEnv(LIVE_NEWS_TRADE_MODE_ENV);
    }

    private static String getLiveNewsEventTag() {
        String env = System.getenv(LIVE_NEWS_EVENT_ENV);
        if (env != null && !env.isBlank()) {
            return env.trim();
        }
        String apiUrl = fetchForexNewsCalendarApiUrl();
        return apiUrl != null ? getActiveHighImpactNewsEventTag(apiUrl) : null;
    }

    // ===============================
    // LABEL (UP/DOWN) with threshold to avoid noise
    // ===============================
    public static int createLabel(java.util.List<Candle> data, int index) {
        if (index + 20 >= data.size()) return -1;
        
        double current = data.get(index).close;
        double atr = calculateATR(data, index, 14);

        double entry = current;
        // Strict institutional targets (1:2 Risk/Reward)
        double stopLossBuy = entry - 1.5 * atr;
        double takeProfitBuy = entry + 3.0 * atr;
        double stopLossSell = entry + 1.5 * atr;
        double takeProfitSell = entry - 3.0 * atr;
        int maxBars = 25;

        for (int j = index + 1; j <= index + maxBars && j < data.size(); j++) {
            double high = data.get(j).high;
            double low = data.get(j).low;

            // Priority: Check what is hit first in temporal order
            
            // Bullish Movement
            if (high >= takeProfitBuy) {
                return 1; // Price reached bullish target
            }
            if (high >= stopLossSell) {
                return 1; // Price reached bullish target (hit sell SL)
            }
            
            // Bearish Movement
            if (low <= takeProfitSell) {
                return 0; // Price reached bearish target
            }
            if (low <= stopLossBuy) {
                return 0; // Price reached bearish target (hit buy SL)
            }
        }
        return -1;
    }

    // ===============================
    // RSI
    // ===============================
    public static double calculateRSI(java.util.List<Candle> data, int index, int period) {

        if (index < period) {
            return 50.0;
        }

        double gain = 0, loss = 0;

        for (int i = index - period; i < index; i++) {
            double diff = data.get(i + 1).close - data.get(i).close;

            if (diff > 0) {
                gain += diff;
            } else {
                loss -= diff;
            }
        }

        if (loss == 0) {
            return 100;
        }

        double rs = gain / loss;
        return 100 - (100 / (1 + rs));
    }

    // ===============================
    // MOVING AVERAGE
    // ===============================
    public static double movingAverage(java.util.List<Candle> data, int index, int period) {

        double sum = 0;

        for (int i = index - period; i < index; i++) {
            sum += data.get(i).close;
        }

        return sum / period;
    }

    // STANDARD DEVIATION (VOLATILITY)
    // ===============================
    public static double stdDev(java.util.List<Candle> data, int index, int period) {

        double mean = movingAverage(data, index, period);
        double sum = 0;

        for (int i = index - period; i < index; i++) {
            double diff = data.get(i).close - mean;
            sum += diff * diff;
        }

        return Math.sqrt(sum / period);
    }

    // ===============================
    // EMA (Exponential Moving Average)
    // ===============================
    public static double calculateEMA(java.util.List<Candle> data, int index, int period) {
        double multiplier = 2.0 / (period + 1);
        double ema = data.get(index - period + 1).close; // initial SMA approximation

        for (int i = index - period + 2; i <= index; i++) {
            ema = (data.get(i).close - ema) * multiplier + ema;
        }

        return ema;
    }

    // ===============================
    // MACD (Moving Average Convergence Divergence)
    // ===============================
    public static double calculateMACD(java.util.List<Candle> data, int index) {
        double ema12 = calculateEMA(data, index, 12);
        double ema26 = calculateEMA(data, index, 26);
        return ema12 - ema26;
    }

    // ===============================
    // BOLLINGER BANDS %B
    // ===============================
    public static double calculateBollingerPercent(java.util.List<Candle> data, int index, int period) {
        double sma = movingAverage(data, index, period);
        double std = stdDev(data, index, period);
        double upper = sma + 2 * std;
        double lower = sma - 2 * std;
        double close = data.get(index).close;
        return (close - lower) / (upper - lower);
    }

    public static double calculateBollingerWidth(java.util.List<Candle> data, int index, int period) {
        double sma = movingAverage(data, index, period);
        double std = stdDev(data, index, period);
        double upper = sma + 2 * std;
        double lower = sma - 2 * std;
        return lower == upper ? 0 : (upper - lower) / Math.max(Math.abs(sma), 1e-6);
    }

    public static double calculateTrendSlope(java.util.List<Candle> data, int index, int period) {
        if (index - period < 0) {
            return 0;
        }
        double start = data.get(index - period).close;
        double end = data.get(index).close;
        return (end - start) / Math.max(period, 1);
    }

    // ===============================
    // STOCHASTIC %K
    // ===============================
    public static double calculateStochasticK(java.util.List<Candle> data, int index, int period) {
        double highest = Double.MIN_VALUE;
        double lowest = Double.MAX_VALUE;

        for (int i = index - period + 1; i <= index; i++) {
            highest = Math.max(highest, data.get(i).high);
            lowest = Math.min(lowest, data.get(i).low);
        }

        double close = data.get(index).close;
        return 100 * (close - lowest) / (highest - lowest);
    }

    // ===============================
    // ATR (Average True Range)
    // ===============================
    public static double calculateATR(java.util.List<Candle> data, int index, int period) {
        double sum = 0;

        for (int i = index - period + 1; i <= index; i++) {
            if (i <= 0) {
                continue;
            }
            double hl = data.get(i).high - data.get(i).low;
            double hc = Math.abs(data.get(i).high - data.get(i - 1).close);
            double lc = Math.abs(data.get(i).low - data.get(i - 1).close);
            double tr = Math.max(hl, Math.max(hc, lc));
            sum += tr;
        }

        return sum / period;
    }

    public static double toPips(String symbol, double priceDifference) {
        return convertPriceDiffToPips(symbol, priceDifference);
    }

    public static double pipsToUsd(String symbol, double pips, double lotSize) {
        return pips * getPipValue(symbol) * lotSize;
    }

    // ===============================
    // CCI (Commodity Channel Index)
    // ===============================
    public static double calculateCCI(java.util.List<Candle> data, int index, int period) {
        double sumTP = 0;

        for (int i = index - period + 1; i <= index; i++) {
            double tp = (data.get(i).high + data.get(i).low + data.get(i).close) / 3;
            sumTP += tp;
        }

        double smaTP = sumTP / period;

        double sumDev = 0;

        for (int i = index - period + 1; i <= index; i++) {
            double tp = (data.get(i).high + data.get(i).low + data.get(i).close) / 3;
            sumDev += Math.abs(tp - smaTP);
        }

        double md = sumDev / period;

        double tp = (data.get(index).high + data.get(index).low + data.get(index).close) / 3;

        return (tp - smaTP) / (0.015 * md);
    }

    // ===============================
    // SMART MONEY CONCEPT (SMC)
    // ===============================
    // Order Block Detection - identifies institutional support/resistance levels
    public static double detectOrderBlock(java.util.List<Candle> data, int index, int lookback) {
        if (index < lookback || index >= data.size()) {
            return 0;
        }

        double atr = calculateATR(data, index, 14);
        double strength = 0;

        for (int i = index - lookback; i < index - 1; i++) {
            if (i < 1 || i + 1 >= data.size()) {
                continue;
            }
            Candle base = data.get(i);
            Candle next = data.get(i + 1);
            double body = Math.abs(base.close - base.open);
            double impulse = Math.abs(next.close - base.close);

            if (body < atr * 0.25 || impulse < atr * 0.25) {
                continue;
            }

            double volumeFactor = base.volume > 0 ? next.volume / Math.max(base.volume, 1.0) : 1.0;
            boolean strongVolume = volumeFactor >= 1.05;
            boolean isBullishBlock = base.close < base.open && next.close > next.open && next.close > base.open + atr * 0.10 && strongVolume;
            boolean isBearishBlock = base.close > base.open && next.close < next.open && next.close < base.open - atr * 0.10 && strongVolume;
            if (!isBullishBlock && !isBearishBlock) {
                continue;
            }

            boolean overlapsCurrent = data.get(index).low <= base.high && data.get(index).high >= base.low;
            boolean nearCurrent = data.get(index).low <= base.high + atr * 0.30 && data.get(index).high >= base.low - atr * 0.30;

            if (overlapsCurrent) {
                strength = Math.max(strength, 0.90);
            } else if (nearCurrent) {
                strength = Math.max(strength, 0.60);
            } else {
                strength = Math.max(strength, 0.30);
            }
        }

        return Math.min(strength, 1.0);
    }

    // Liquidity Zone Detection - identifies where price may return for institutional orders
    public static double detectLiquidityZone(java.util.List<Candle> data, int index, int period) {
        if (index < period || index >= data.size()) {
            return 0;
        }

        double atr = calculateATR(data, index, 14);
        double score = 0;
        int points = 0;

        for (int i = index - period + 1; i < index; i++) {
            if (i <= 0) {
                continue;
            }
            double high = data.get(i).high;
            double low = data.get(i).low;
            double prevHigh = data.get(i - 1).high;
            double prevLow = data.get(i - 1).low;

            if (Math.abs(high - prevHigh) < atr * 0.1) {
                score += 0.25;
            }
            if (Math.abs(low - prevLow) < atr * 0.1) {
                score += 0.25;
            }
            if (Math.abs(high - prevLow) < atr * 0.05 || Math.abs(low - prevHigh) < atr * 0.05) {
                score += 0.15;
            }
            if (Math.abs(data.get(i).high - data.get(i).low) < atr * 0.7) {
                score += 0.10;
            }
            points += 1;
        }

        if (points == 0) {
            return 0;
        }
        double liquidityScore = Math.min(1.0, score / points);
        return liquidityScore;
    }

    // Fair Value Gap Detection - identifies gaps where price may return
    public static double detectFairValueGap(java.util.List<Candle> data, int index) {
        if (index < 3 || index >= data.size()) return 0;

        // UPGRADE 2: Standard 3-Candle Institutional FVG Detection
        // Bullish FVG: Gap between Candle 1 High and Candle 3 Low
        if (data.get(index - 2).high < data.get(index).low) {
            double gap = data.get(index).low - data.get(index - 2).high;
            return Math.min(1.0, gap / (data.get(index).close * 0.005)); // Normalized score
        }
        
        // Bearish FVG: Gap between Candle 1 Low and Candle 3 High
        if (data.get(index - 2).low > data.get(index).high) {
            double gap = data.get(index - 2).low - data.get(index).high;
            return -Math.min(1.0, gap / (data.get(index).close * 0.005)); // Negative for bearish
        }

        return 0;
    }

    // Market Structure - Higher Highs/Lows (Uptrend) or Lower Highs/Lows (Downtrend)
    public static int detectMarketStructure(java.util.List<Candle> data, int index, int lookback) {
        if (index < lookback || index >= data.size()) {
            return 0;
        }

        java.util.List<Integer> swingHighIndexes = new ArrayList<>();
        java.util.List<Integer> swingLowIndexes = new ArrayList<>();

        for (int i = Math.max(1, index - lookback); i < index; i++) {
            if (i + 1 >= data.size()) {
                break;
            }
            Candle prev = data.get(i - 1);
            Candle current = data.get(i);
            Candle next = data.get(i + 1);

            if (current.high > prev.high && current.high > next.high) {
                swingHighIndexes.add(i);
            }
            if (current.low < prev.low && current.low < next.low) {
                swingLowIndexes.add(i);
            }
        }

        if (swingHighIndexes.size() < 2 || swingLowIndexes.size() < 2) {
            return 0;
        }

        int lastHighIndex = swingHighIndexes.get(swingHighIndexes.size() - 1);
        int prevHighIndex = swingHighIndexes.get(swingHighIndexes.size() - 2);
        int lastLowIndex = swingLowIndexes.get(swingLowIndexes.size() - 1);
        int prevLowIndex = swingLowIndexes.get(swingLowIndexes.size() - 2);

        double lastHigh = data.get(lastHighIndex).high;
        double prevHigh = data.get(prevHighIndex).high;
        double lastLow = data.get(lastLowIndex).low;
        double prevLow = data.get(prevLowIndex).low;

        if (lastHigh > prevHigh && lastLow > prevLow) {
            return 1; // Uptrend (HH/HL)
        } else if (lastHigh < prevHigh && lastLow < prevLow) {
            return -1; // Downtrend (LH/LL)
        }

        return 0;
    }

    public static boolean isRangingMarket(java.util.List<Candle> data, int index, int period) {
        if (index < period || index >= data.size()) {
            return false;
        }

        double atr = calculateATR(data, index, 14);
        double high = Double.MIN_VALUE;
        double low = Double.MAX_VALUE;
        double totalRange = 0;

        for (int i = index - period; i < index; i++) {
            high = Math.max(high, data.get(i).high);
            low = Math.min(low, data.get(i).low);
            totalRange += data.get(i).high - data.get(i).low;
        }

        double avgRange = totalRange / period;
        double channelRange = high - low;

        return channelRange < atr * period * 0.8 && avgRange < atr * 1.2;
    }

    public static String detectMarketRegime(java.util.List<Candle> data, int index, int period) {
        if (index < period || index >= data.size()) {
            return "unknown";
        }

        double currentPrice = data.get(index).close;
        double atr = calculateATR(data, index, 14);
        double atrPct = atr / Math.max(currentPrice, 1e-6);
        double ema20 = calculateEMA(data, index, 20);
        double ema50 = calculateEMA(data, index, 50);
        double emaSlope = Math.abs((ema20 - ema50) / Math.max(ema50, 1e-6));

        // --- UPGRADED REGIME DETECTION ---
        if (atrPct > 0.0015) {
            return "VOLATILE";
        }

        if (isRangingMarket(data, index, period)) {
            // Deep ranging analysis: Accumulation vs Distribution
            double rsi = calculateRSI(data, index, 14);
            if (rsi < 45) {
                return "ACCUMULATION";
            }
            if (rsi > 55) {
                return "DISTRIBUTION";
            }
            return "RANGING";
        }

        if (emaSlope > 0.0015) {
            return "TRENDING";
        }

        return "RANGING";
    }

    // Resistance/Support Flip - when key level changes from resistance to support or vice versa
    public static double detectLevelFlip(java.util.List<Candle> data, int index, int period) {
        if (index < period || index >= data.size()) {
            return 0.0;
        }

        double highestHigh = Double.MIN_VALUE;
        double lowestLow = Double.MAX_VALUE;

        for (int i = index - period; i < index; i++) {
            if (i >= 0) {
                highestHigh = Math.max(highestHigh, data.get(i).high);
                lowestLow = Math.min(lowestLow, data.get(i).low);
            }
        }

        double currentClose = data.get(index).close;
        double avgVol = calculateAverageVolume(data, index, 20);
        double flipStrength = 0.0;

        // Resistance becomes support when price breaks above and pulls back
        if (currentClose > highestHigh && data.get(index).volume > avgVol * 0.8) {
            flipStrength = 0.8; // Strong breakout above resistance
        } // Support becomes resistance when price breaks below
        else if (currentClose < lowestLow && data.get(index).volume > avgVol * 0.8) {
            flipStrength = -0.8; // Strong breakdown below support
        }

        return flipStrength;
    }

    // SMC Confluence Score - combines all SMC factors for signal quality
    public static double calculateICTConfluence(java.util.List<Candle> data, int index, int pred) {
        if (index < 50 || index >= data.size()) {
            return 0;
        }

        double orderBlock = detectOrderBlock(data, index, 10);
        double liquidity = detectLiquidityZone(data, index, 10);
        double fvg = detectFairValueGap(data, index);
        double levelFlip = detectLevelFlip(data, index, 20);
        int structure = detectMarketStructure(data, index, 20);

        double ictScore = 0;
        if (orderBlock > 0.75) {
            ictScore += 0.25;
        }
        if (fvg > 0.4) {
            ictScore += 0.20;
        }
        if (liquidity > 0.6) {
            ictScore += 0.15;
        }
        if (pred == 1 && levelFlip > 0.5) {
            ictScore += 0.15;
        }
        if (pred == 0 && levelFlip < -0.5) {
            ictScore += 0.15;
        }
        if ((pred == 1 && structure == 1) || (pred == 0 && structure == -1)) {
            ictScore += 0.10;
        }

        return Math.min(ictScore, 1.0);
    }

    public static double calculateSMCConfluence(java.util.List<Candle> data, int index, int pred, double baseConfidence) {
        if (index < 50 || index >= data.size()) {
            return baseConfidence;
        }

        double orderBlock = detectOrderBlock(data, index, 10);
        double liquidity = detectLiquidityZone(data, index, 10);
        double fvg = detectFairValueGap(data, index);
        double levelFlip = detectLevelFlip(data, index, 20);

        // Boost confidence if SMC factors align
        double smcBoost = 0;

        if (orderBlock > 0.8) {
            smcBoost += 0.10;
        }
        if (liquidity > 0.8) {
            smcBoost += 0.08;
        }
        if (fvg > 0.5) {
            smcBoost += 0.07;
        }
        if (pred == 1 && levelFlip > 0.5) {
            smcBoost += 0.10;
        }
        if (pred == 0 && levelFlip < -0.5) {
            smcBoost += 0.10;
        }

        double ictBoost = calculateICTConfluence(data, index, pred) * 0.4;
        double finalConfidence = baseConfidence + (smcBoost * 0.5) + ictBoost;
        return Math.min(finalConfidence, 1.0);
    }

    // SMC Trading Signal - enhanced entry/SL/TP based on smart money zones
    public static class SMCSignal {

        public String symbol;
        public String direction;
        public double entry;
        public double stopLoss;
        public double takeProfit;
        public double confidence;
        public String smcReason;

        SMCSignal(String sym, String dir, double ent, double sl, double tp, double conf, String reason) {
            symbol = sym;
            direction = dir;
            entry = ent;
            stopLoss = sl;
            takeProfit = tp;
            confidence = conf;
            smcReason = reason;
        }
    }

    // ===============================
    // PROFESSIONAL TRADE SIGNAL
    // ===============================
    public static class TradeSignal {

        public String symbol;
        public String direction;
        public double entry, stopLoss, takeProfit;
        public double mlConfidence, smcConfluence;
        public double signalStrength; // 0-100%
        public String reason;
        public double riskAmount, rewardAmount;
        public double riskRewardRatio;
        public double lotSize = 0.01; // Default
        public double[] features;
        public String regime;
        public int sourceIndex;
        public long timestamp;
        public String session;
        public boolean trendAligned;
        public boolean liquiditySweep;
        public boolean BOS;
        public boolean sessionGood;
        public boolean lowSpread;
        public boolean volumeSpike;
        public int precisionScore;
        public String setupType;
        public double spreadPips;
        public double riskPercent = 0.01;

        TradeSignal(String sym, String dir, double ent, double sl, double tp, double ml, double smc,
                double strength, String reason, double risk, double reward,
                double[] features, String regime, int sourceIndex) {
            symbol = sym;
            direction = dir;
            entry = ent;
            stopLoss = sl;
            takeProfit = tp;
            mlConfidence = ml;
            smcConfluence = smc;
            signalStrength = strength;
            this.reason = reason;
            riskAmount = risk;
            rewardAmount = reward;
            riskRewardRatio = (risk > 0 && reward > 0) ? reward / risk : 0;
            this.features = features;
            this.regime = regime;
            this.sourceIndex = sourceIndex;
            timestamp = System.currentTimeMillis();
        }

        TradeSignal(String sym, String dir, double ent, double sl, double tp, double ml, double smc,
                double strength, String reason, double risk, double reward) {
            this(sym, dir, ent, sl, tp, ml, smc, strength, reason, risk, reward, null, "unknown", -1);
        }

        TradeSignal(String sym, String dir, double ent, double sl, double tp, double ml, double smc,
                double strength, String reason) {
            this(sym, dir, ent, sl, tp, ml, smc, strength, reason, 10.0, 30.0, null, "unknown", -1);
        }

        TradeSignal(String dir, double ent, double sl, double tp, double ml, double smc,
                double strength, String reason, double risk, double reward) {
            this("EURUSD", dir, ent, sl, tp, ml, smc, strength, reason, risk, reward, null, "unknown", -1);
        }
    }

    public static class LiveTrade {

        public TradeSignal signal;
        public String status;
        public double entryPrice;
        public double stopLoss;
        public double takeProfit;
        public double lotSize;
        public double riskPips;
        public double rewardPips;
        public boolean breakevenSet;
        public boolean partialTaken;
        public int candlesInTrade;
        public int entryIndex;
        public double profitPips;
        public String exitReason;
        public double exitPrice;

        LiveTrade(TradeSignal signal, double entryPrice, double stopLoss, double takeProfit,
                double lotSize, double riskPips, double rewardPips, int entryIndex) {
            this.signal = signal;
            this.status = "OPEN";
            this.entryPrice = entryPrice;
            this.stopLoss = stopLoss;
            this.takeProfit = takeProfit;
            this.lotSize = lotSize;
            this.riskPips = riskPips;
            this.rewardPips = rewardPips;
            this.entryIndex = entryIndex;
            this.breakevenSet = false;
            this.partialTaken = false;
            this.candlesInTrade = 0;
            this.profitPips = 0;
        }

        public double pnlUsd;

        public boolean isOpen() {
            return "OPEN".equals(status);
        }

        public void update(Candle candle) {
            if (!isOpen()) {
                return;
            }
            candlesInTrade++;
            double currentClose = candle.close;
            double currentPips = signal.direction.equals("BUY") ? toPips(signal.symbol, currentClose - entryPrice) : toPips(signal.symbol, entryPrice - currentClose);
            profitPips = currentPips;

            // --- QUANTUM FUSION ENGINE: SMART TRADE MANAGEMENT ---

            // 1. 1R -> Move to Break Even
            if (!breakevenSet && currentPips >= riskPips * 1.0) {
                if (signal.direction.equals("BUY")) {
                    stopLoss = Math.max(stopLoss, entryPrice);
                } else {
                    stopLoss = Math.min(stopLoss, entryPrice);
                }
                breakevenSet = true;
                System.out.println("🛡️ [QFE Management] BE set for " + signal.symbol);
            }

            // 2. 2R -> Signal partial profit (In logic only here)
            if (!partialTaken && currentPips >= riskPips * 2.0) {
                System.out.println("💰 [QFE Management] 2R reached. Partial profit target hit for " + signal.symbol);
                partialTaken = true;
            }

            // 3. 3R+ -> Dynamic ATR Trailing
            if (currentPips >= riskPips * 3.0) {
                double buffer = convertPipsToPrice(signal.symbol, riskPips * 0.5);
                if (signal.direction.equals("BUY")) {
                    stopLoss = Math.max(stopLoss, currentClose - buffer);
                } else {
                    stopLoss = Math.min(stopLoss, currentClose + buffer);
                }
            }

            // ATR-scaled dynamic timeout
            double atr = convertPipsToPrice(signal.symbol, signal.riskAmount);
            double currentPrice = candle.close;
            double atrPct = currentPrice > 0 ? atr / currentPrice : 0.001;
            int maxCandles = Math.max(8, Math.min(50, (int) (20.0 / Math.max(atrPct, 0.0001))));

            if (candlesInTrade > maxCandles) {
                close(candle.close, "Timeout Exit (ATR-scaled: " + maxCandles + " bars)");
                return;
            }

            if (signal.direction.equals("BUY")) {
                if (candle.high >= takeProfit) {
                    close(takeProfit, "TP Hit");
                } else if (candle.low <= stopLoss) {
                    close(stopLoss, "SL/Trailing Hit");
                }
            } else {
                if (candle.low <= takeProfit) {
                    close(takeProfit, "TP Hit");
                } else if (candle.high >= stopLoss) {
                    close(stopLoss, "SL/Trailing Hit");
                }
            }
        }

        public void close(double price, String reason) {
            if (!isOpen()) {
                return;
            }
            this.exitPrice = price;
            this.exitReason = reason;
            this.status = "CLOSED";
            double finalPips = signal.direction.equals("BUY") ? toPips(signal.symbol, price - entryPrice) : toPips(signal.symbol, entryPrice - price);
            this.profitPips = finalPips;
            this.pnlUsd = finalPips * lotSize * getPipValue(signal.symbol);
            System.out.printf("📉 Trade Closed [%s]: %s Pips=%.1f PNL=$%.2f%n", signal.symbol, reason, finalPips, pnlUsd);
        }
    }

    public static class TradeRecord {

        public long timestamp;
        public String symbol;
        public String direction;
        public double entry;
        public double stopLoss;
        public double takeProfit;
        public double confidence;
        public double signalStrength;
        public double riskPips;
        public double riskRewardRatio;
        public double rewardPips;
        public double lotSize;
        public String regime;
        public int sourceIndex;
        public double resultPips;
        public String outcome;
        public String exitReason;
        public String status;
        public int duration;
        public double pnlUsd;
        public String session;
        public String setupType;
        public boolean trendAligned;
        public double spreadPips;
        public int precisionScore;
        
        // --- AI LEARNING ENGINE DATA ---
        public double fvgSize;
        public double sweepSize;
        public boolean hasBOS;
        public boolean hasCHoCH;
        public double obStrength;
        public double atr;
        public double momentum;

        TradeRecord(TradeSignal signal, LiveTrade trade) {
            this.timestamp = signal.timestamp;
            this.symbol = signal.symbol;
            this.direction = signal.direction;
            this.entry = trade.entryPrice;
            this.stopLoss = trade.stopLoss;
            this.takeProfit = trade.takeProfit;
            this.confidence = signal.mlConfidence;
            this.signalStrength = signal.signalStrength;
            this.riskPips = trade.riskPips;
            this.rewardPips = trade.rewardPips;
            this.riskRewardRatio = trade.rewardPips > 0 ? trade.rewardPips / Math.max(1.0, trade.riskPips) : 0.0;
            this.lotSize = trade.lotSize;
            this.regime = signal.regime;
            this.sourceIndex = signal.sourceIndex;
            this.resultPips = trade.profitPips;
            this.outcome = trade.profitPips > 0 ? "WIN" : (trade.profitPips < 0 ? "LOSS" : "BREAKEVEN");
            this.exitReason = trade.exitReason;
            this.status = trade.status;
            this.duration = trade.candlesInTrade;
            this.pnlUsd = trade.profitPips * trade.lotSize * getPipValue(trade.signal.symbol);
            this.session = signal.session;
            this.setupType = signal.setupType;
            this.trendAligned = signal.trendAligned;
            this.spreadPips = signal.spreadPips;
            this.precisionScore = signal.precisionScore;
            
            // Capture engine states for learning
            this.fvgSize = detectFairValueGap(Collections.singletonList(new Candle(0,0,0,0,0,0)), 0); // Placeholder, real engine would pass these
            this.obStrength = detectOrderBlock(null, 0, 0); 
        }

        TradeRecord(TradeSignal signal, String status, String exitReason) {
            this.timestamp = signal.timestamp;
            this.symbol = signal.symbol;
            this.direction = signal.direction;
            this.entry = signal.entry;
            this.stopLoss = signal.stopLoss;
            this.takeProfit = signal.takeProfit;
            this.confidence = signal.mlConfidence;
            this.signalStrength = signal.signalStrength;
            this.riskPips = signal.riskAmount;
            this.rewardPips = signal.rewardAmount;
            this.riskRewardRatio = signal.rewardAmount > 0 ? signal.rewardAmount / Math.max(1.0, signal.riskAmount) : 0.0;
            this.lotSize = Math.max(0.01, Math.min(1.0, DEFAULT_ACCOUNT_BALANCE * DEFAULT_RISK_PERCENT / Math.max(1.0, signal.riskAmount * getPipValue(signal.symbol))));
            this.regime = signal.regime;
            this.sourceIndex = signal.sourceIndex;
            this.resultPips = 0;
            this.outcome = "OPEN";
            this.exitReason = exitReason;
            this.status = status;
            this.duration = 0;
            this.pnlUsd = 0;
            this.session = signal.session;
            this.setupType = signal.setupType;
            this.trendAligned = signal.trendAligned;
            this.spreadPips = signal.spreadPips;
            this.precisionScore = signal.precisionScore;
        }

        public String toCsvLine() {
            return String.format("%d,%s,%s,%.5f,%.5f,%.5f,%.4f,%.2f,%.2f,%.2f,%.2f,%s,%d,%.2f,%.2f,%s,%s,%s,%s,%b,%.2f,%d,%.5f,%.5f,%b,%b,%.2f,%.5f,%.5f",
                    timestamp, safe(symbol), safe(direction), entry, stopLoss, takeProfit, confidence, signalStrength,
                    riskPips, rewardPips, riskRewardRatio, lotSize, safe(regime), sourceIndex, resultPips, pnlUsd, safe(exitReason), safe(status),
                    safe(session), safe(setupType), trendAligned, spreadPips, precisionScore,
                    fvgSize, sweepSize, hasBOS, hasCHoCH, obStrength, atr, momentum);
        }

        private static String safe(String input) {
            return input == null ? "" : input.replace(",", " ").replace("\n", " ").replace("\r", " ");
        }
    }

    public static class TradeExecutionEngine {

        public java.util.List<LiveTrade> openTrades = new ArrayList<>();
        public java.util.List<TradeRecord> tradeLog = new ArrayList<>();
        public double maxSpreadPips = 2.0;
        public int maxOpenTrades = 3;
        public double riskPerTradeUsd;
        public double accountBalanceUsd;
        public int consecutiveLosses;
        public double peakBalanceUsd;
        public double dailyLossLimitPct = 0.05;
        public double maxDrawdownPct = 0.10;

        TradeExecutionEngine(double accountBalanceUsd, double riskPerTradeUsd) {
            this.accountBalanceUsd = accountBalanceUsd;
            this.peakBalanceUsd = accountBalanceUsd;
            this.riskPerTradeUsd = riskPerTradeUsd;
        }

        public boolean canTrade() {
            double currentDrawdown = (peakBalanceUsd - accountBalanceUsd) / peakBalanceUsd;
            return currentDrawdown < maxDrawdownPct && consecutiveLosses < 5;
        }

        public double getCurrentSpreadPips() {
            String env = System.getenv("CURRENT_SPREAD_PIPS");
            if (env != null && !env.isEmpty()) {
                try {
                    return Double.parseDouble(env);
                } catch (NumberFormatException ignored) {
                }
            }
            return 1.2;
        }

        public boolean isHighImpactNewsWindow() {
            return Fxausd.isHighImpactNewsWindow();
        }

        public void executeSignals(java.util.List<TradeSignal> signals, java.util.List<Candle> candles) {
            if (!isWithinActiveForexSession()) {
                System.out.println("⚠️ Outside active London/NY session; live execution paused.");
                return;
            }
            boolean newsWindow = isHighImpactNewsWindow();
            String newsTag = newsWindow ? getLiveNewsEventTag() : null;

            for (TradeSignal signal : signals) {
                if (!canTrade()) {
                    System.out.println("⚠️ Trading paused due to risk limits or drawdown.");
                    break;
                }
                if (openTrades.size() >= maxOpenTrades) {
                    System.out.println("⚠️ Max open trades reached, skipping signal.");
                    continue;
                }
                if (newsWindow && !isLiveNewsTradeMode()) {
                    System.out.println("⚠️ High impact news window detected, skipping live trade" + (newsTag != null ? ": " + newsTag : "."));
                    continue;
                }
                if (newsWindow && isLiveNewsTradeMode()) {
                    System.out.println("⚠️ High impact news window active, executing news-driven trade" + (newsTag != null ? ": " + newsTag : "."));
                }
                double spreadPips = getCurrentSpreadPips();
                if (spreadPips > maxSpreadPips) {
                    System.out.println("⚠️ Spread too wide (" + spreadPips + " pips), skipping trade.");
                    continue;
                }
                if (signal.riskAmount <= 0) {
                    System.out.println("⚠️ Invalid risk amount, skipping trade.");
                    continue;
                }
                double lotSize = Math.max(0.01, Math.min(1.0, riskPerTradeUsd / (signal.riskAmount * getPipValue(signal.symbol))));
                if (lotSize <= 0) {
                    System.out.println("⚠️ Position sizing failed, skipping trade.");
                    continue;
                }
                signal.lotSize = lotSize; // Update signal with calculated lot
                int entryIndex = Math.min(signal.sourceIndex + 1, candles.size() - 1);
                Candle entryCandle = candles.get(entryIndex);
                double entryPrice = entryCandle.open + (signal.direction.equals("BUY") ? convertPipsToPrice(signal.symbol, spreadPips) : -convertPipsToPrice(signal.symbol, spreadPips));
                double adjustedStopLoss = signal.stopLoss + (entryPrice - signal.entry);
                double adjustedTakeProfit = signal.takeProfit + (entryPrice - signal.entry);
                boolean sent = sendSignalToMT5(signal);
                if (!sent) {
                    System.out.println("❌ Failed live trade dispatch: " + signal.symbol + " " + signal.direction + " @ " + String.format("%.5f", entryPrice));
                    continue;
                }
                LiveTrade trade = new LiveTrade(signal, entryPrice, adjustedStopLoss, adjustedTakeProfit,
                        lotSize, signal.riskAmount, signal.rewardAmount, entryIndex);
                openTrades.add(trade);
                System.out.println("📈 Executed live trade: " + signal.symbol + " " + signal.direction + " @ " + String.format("%.5f", entryPrice));
                processFutureBars(candles, signal.sourceIndex + 1);
                if (!trade.isOpen()) {
                    TradeRecord record = new TradeRecord(signal, trade);
                    tradeLog.add(record);
                    recordTrade(record);
                    accountBalanceUsd += trade.pnlUsd;
                    peakBalanceUsd = Math.max(peakBalanceUsd, accountBalanceUsd);
                    if (trade.profitPips < 0) {
                        consecutiveLosses++;
                    } else {
                        consecutiveLosses = 0;
                    }
                }
            }
            for (LiveTrade open : new ArrayList<>(openTrades)) {
                if (!open.isOpen()) {
                    TradeRecord record = new TradeRecord(open.signal, open);
                    tradeLog.add(record);
                    recordTrade(record);
                }
            }
            exportTradeLogToCSV(tradeLog, "trade_history.csv");
        }

        public void processFutureBars(java.util.List<Candle> candles, int startIndex) {
            for (int i = startIndex; i < candles.size(); i++) {
                Candle candle = candles.get(i);
                for (LiveTrade trade : new ArrayList<>(openTrades)) {
                    if (!trade.isOpen()) {
                        continue;
                    }
                    trade.update(candle);
                    if (!trade.isOpen()) {
                        openTrades.remove(trade);
                    }
                }
                if (openTrades.isEmpty()) {
                    break;
                }
            }
        }
    }

    public static void exportTradeLogToCSV(java.util.List<TradeRecord> records, String filename) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(filename))) {
            writer.println("Timestamp,Symbol,Direction,Entry,StopLoss,TakeProfit,Confidence,SignalStrength,RiskPips,RewardPips,RR,LotSize,Regime,SourceIndex,ResultPips,PNLUSD,ExitReason,Status,Session,SetupType,TrendAligned,SpreadPips,PrecisionScore");
            for (TradeRecord record : records) {
                writer.println(record.toCsvLine());
            }
            System.out.println("✅ Trade history exported to: " + filename);
        } catch (IOException e) {
            System.out.println("❌ Error exporting trade history: " + e.getMessage());
        }
    }

    public static class TradeDatabase {

        private boolean enabled;
        private String url;
        private String user;
        private String password;
        private String driverClass;
        private String tableName;

        // Simple cache to avoid slamming the DB during audit scans
        private final Map<String, Double> winRateCache = new HashMap<>();
        private final Map<String, Double> pairPerfCache = new HashMap<>();
        private long lastCacheClear = 0;

        public void initialize() {
            // Set Neon.tech as the default database with increased timeouts
            url = "jdbc:postgresql://ep-summer-cloud-apjyv5uu-pooler.c-7.us-east-1.aws.neon.tech/neondb?sslmode=require&user=neondb_owner&password=npg_jx0YXqbu6sAV&loginTimeout=15&connectTimeout=15&socketTimeout=30";
            driverClass = "org.postgresql.Driver";
            tableName = "trade_history";

            try {
                Class.forName(driverClass);
            } catch (ClassNotFoundException e) {
                System.out.println("⚠️ Trade DB driver class not found: " + e.getMessage());
            }
            try (Connection conn = createConnection()) {
                if (conn != null) {
                    createTableIfNeeded(conn);
                    enabled = true;
                    System.out.println("✅ Trade logging enabled to database: Neon.tech (neondb) table=" + tableName);
                }
            } catch (Exception e) {
                enabled = false;
                System.out.println("⚠️ Trade database initialization failed: " + e.getMessage());
            }
        }

        public boolean isEnabled() {
            return enabled;
        }

        private Connection createConnection() throws SQLException {
            if (user != null && !user.trim().isEmpty()) {
                return DriverManager.getConnection(url, user, password);
            }
            return DriverManager.getConnection(url);
        }

        private void createTableIfNeeded(Connection conn) throws SQLException {
            String sql = "CREATE TABLE IF NOT EXISTS " + tableName + " ("
                    + "timestamp BIGINT NOT NULL, "
                    + "symbol VARCHAR(64), "
                    + "direction VARCHAR(8), "
                    + "entry DOUBLE PRECISION, "
                    + "stop_loss DOUBLE PRECISION, "
                    + "take_profit DOUBLE PRECISION, "
                    + "confidence DOUBLE PRECISION, "
                    + "signal_strength DOUBLE PRECISION, "
                    + "risk_pips DOUBLE PRECISION, "
                    + "reward_pips DOUBLE PRECISION, "
                    + "rr DOUBLE PRECISION, "
                    + "lot_size DOUBLE PRECISION, "
                    + "regime VARCHAR(128), "
                    + "source_index INTEGER, "
                    + "result_pips DOUBLE PRECISION, "
                    + "outcome VARCHAR(32), "
                    + "exit_reason VARCHAR(256), "
                    + "status VARCHAR(32), "
                    + "duration INTEGER, "
                    + "pnl_usd DOUBLE PRECISION, "
                    + "session VARCHAR(32), "
                    + "setup_type VARCHAR(128), "
                    + "trend_aligned BOOLEAN, "
                    + "spread_pips DOUBLE PRECISION, "
                    + "precision_score INTEGER"
                    + ")";
            try (Statement stmt = conn.createStatement()) {
                stmt.execute(sql);
                // 1. Setup trade_stats table
                stmt.execute("CREATE TABLE IF NOT EXISTS trade_stats ("
                        + "setup_type VARCHAR(64) PRIMARY KEY, "
                        + "wins INTEGER DEFAULT 0, "
                        + "losses INTEGER DEFAULT 0)");

                // 8. Setup pair_performance table
                stmt.execute("CREATE TABLE IF NOT EXISTS pair_performance ("
                        + "symbol VARCHAR(64) PRIMARY KEY, "
                        + "wins INTEGER DEFAULT 0, "
                        + "losses INTEGER DEFAULT 0, "
                        + "profit_factor DOUBLE PRECISION DEFAULT 1.0, "
                        + "drawdown DOUBLE PRECISION DEFAULT 0.0)");
            }
        }

        public double getHistoricalWinRate(String setupType) {
            if (!enabled) {
                return 0.75;
            }

            // Clear cache every 10 minutes
            if (System.currentTimeMillis() - lastCacheClear > 600000) {
                winRateCache.clear();
                pairPerfCache.clear();
                lastCacheClear = System.currentTimeMillis();
            }

            if (winRateCache.containsKey(setupType)) {
                return winRateCache.get(setupType);
            }

            String sql = "SELECT wins, losses FROM trade_stats WHERE setup_type = ?";
            try (Connection conn = createConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, setupType);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    int wins = rs.getInt("wins");
                    int losses = rs.getInt("losses");
                    double rate = (double) wins / Math.max(1, wins + losses);
                    winRateCache.put(setupType, rate);
                    return rate;
                }
            } catch (Exception e) {
                System.out.println("⚠️ DB WinRate fetch error: " + e.getMessage());
            }
            return 0.75;
        }

        public double getPairPerformanceMult(String symbol) {
            if (!enabled) {
                return 1.0;
            }

            if (pairPerfCache.containsKey(symbol)) {
                return pairPerfCache.get(symbol);
            }

            String sql = "SELECT wins, losses FROM pair_performance WHERE symbol = ?";
            try (Connection conn = createConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, symbol);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    int wins = rs.getInt("wins");
                    int losses = rs.getInt("losses");
                    double winRate = (double) wins / Math.max(1, wins + losses);
                    double mult = winRate < 0.55 ? 0.85 : 1.0;
                    pairPerfCache.put(symbol, mult);
                    return mult;
                }
            } catch (Exception e) {
                System.out.println("⚠️ DB PairPerf fetch error: " + e.getMessage());
            }
            return 1.0;
        }

        public boolean insertTrade(TradeRecord record) {
            if (!enabled || record == null) {
                return false;
            }
            String sql = "INSERT INTO " + tableName + " (timestamp, symbol, direction, entry, stop_loss, take_profit, confidence, signal_strength, risk_pips, reward_pips, rr, lot_size, regime, source_index, result_pips, outcome, exit_reason, status, duration, pnl_usd, session, setup_type, trend_aligned, spread_pips, precision_score) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            try (Connection conn = createConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setLong(1, record.timestamp);
                ps.setString(2, record.symbol);
                ps.setString(3, record.direction);
                ps.setDouble(4, record.entry);
                ps.setDouble(5, record.stopLoss);
                ps.setDouble(6, record.takeProfit);
                ps.setDouble(7, record.confidence);
                ps.setDouble(8, record.signalStrength);
                ps.setDouble(9, record.riskPips);
                ps.setDouble(10, record.rewardPips);
                ps.setDouble(11, record.riskRewardRatio);
                ps.setDouble(12, record.lotSize);
                ps.setString(13, record.regime);
                ps.setInt(14, record.sourceIndex);
                ps.setDouble(15, record.resultPips);
                ps.setString(16, record.outcome);
                ps.setString(17, record.exitReason);
                ps.setString(18, record.status);
                ps.setInt(19, record.duration);
                ps.setDouble(20, record.pnlUsd);
                ps.setString(21, record.session);
                ps.setString(22, record.setupType);
                ps.setBoolean(23, record.trendAligned);
                ps.setDouble(24, record.spreadPips);
                ps.setInt(25, record.precisionScore);
                ps.executeUpdate();
                return true;
            } catch (Exception e) {
                System.out.println("⚠️ Trade database insert failed: " + e.getMessage());
                return false;
            }
        }
    }

    public static void startDashboardServer() {
        if (parseBooleanEnv(DISABLE_DASHBOARD_SERVER_ENV)) {
            System.out.println("⚠️ Dashboard disabled by " + DISABLE_DASHBOARD_SERVER_ENV + ".");
            return;
        }

        int port = Integer.parseInt(System.getenv().getOrDefault(DASHBOARD_PORT_ENV, DEFAULT_DASHBOARD_PORT));
        if (!isPortAvailable(port)) {
            System.out.println("⚠️ Dashboard port " + port + " is unavailable. Dashboard startup skipped.");
            return;
        }

        try {
            dashboardServer = HttpServer.create(new InetSocketAddress(port), 0);
            dashboardServer.createContext("/", exchange -> {
                String html = "<html><head><title>ForexBot Dashboard</title><style>"
                        + "body{font-family:Arial,sans-serif;margin:20px;background:#121212;color:#e8e8e8;}"
                        + "h1,h2{color:#fff;}"
                        + "button{padding:10px 16px;margin:8px 0;border:none;border-radius:6px;background:#2563eb;color:#fff;cursor:pointer;}"
                        + "button:hover{background:#1d4ed8;}"
                        + ".panel{background:#1e293b;border:1px solid #334155;border-radius:12px;padding:16px;margin-bottom:20px;}"
                        + "table{border-collapse:collapse;width:100%;margin-top:12px;}"
                        + "th,td{border:1px solid #334155;padding:10px;text-align:left;}"
                        + "th{background:#0f172a;}"
                        + "pre{background:#0f172a;color:#e2e8f0;padding:12px;border-radius:10px;overflow:auto;}"
                        + "</style></head><body>"
                        + "<h1>ForexBot Dashboard</h1>"
                        + "<p>Live generated signals, recent trades, and system status.</p>"
                        + "<button onclick=\"refreshData()\">Refresh Dashboard</button>"
                        + "<div class=\"panel\"><h2>Live Signals</h2><div id=\"signals\">Loading signals...</div></div>"
                        + "<div class=\"panel\"><h2>Recent Trades</h2><div id=\"trades\">Loading trades...</div></div>"
                        + "<div class=\"panel\"><h2>Raw Status</h2><pre id=\"status\">Loading status...</pre></div>"
                        + "<p>Use <a href=\"/api/status\">/api/status</a>, <a href=\"/api/signals\">/api/signals</a>, <a href=\"/api/trades\">/api/trades</a></p>"
                        + "<script>"
                        + "async function fetchJson(url){const res=await fetch(url);if(!res.ok){throw new Error('HTTP '+res.status);}return res.json();}"
                        + "function renderSignals(signals){const container=document.getElementById('signals');if(!Array.isArray(signals)||signals.length===0){container.innerHTML='<p>No active signals available.</p>';return;}"
                        + "const rows=signals.map(s=>'<tr>'+'<td>'+escapeHtml(s.symbol)+'</td>'+'<td>'+escapeHtml(s.direction)+'</td>'+'<td>'+escapeHtml(s.regime||'n/a')+'</td>'+'<td>'+formatNumber(s.entry)+'</td>'+'<td>'+formatNumber(s.stopLoss)+'</td>'+'<td>'+formatNumber(s.takeProfit)+'</td>'+'<td>'+formatNumber(s.signalStrength)+'</td>'+'<td>'+formatNumber(s.confidence)+'</td>'+'</tr>').join('');"
                        + "container.innerHTML='<table><thead><tr><th>Symbol</th><th>Direction</th><th>Regime</th><th>Entry</th><th>SL</th><th>TP</th><th>Strength</th><th>Confidence</th></tr></thead><tbody>'+rows+'</tbody></table>'; }"
                        + "function renderTrades(trades){const container=document.getElementById('trades');if(!Array.isArray(trades)||trades.length===0){container.innerHTML='<p>No recent trades logged.</p>';return;}"
                        + "const rows=trades.map(t=>'<tr>'+'<td>'+new Date(t.timestamp).toLocaleString()+'</td>'+'<td>'+escapeHtml(t.symbol)+'</td>'+'<td>'+escapeHtml(t.direction)+'</td>'+'<td>'+formatNumber(t.entry)+'</td>'+'<td>'+formatNumber(t.exit)+'</td>'+'<td>'+formatNumber(t.pnlUsd)+'</td>'+'<td>'+escapeHtml(t.status||'n/a')+'</td>'+'</tr>').join('');"
                        + "container.innerHTML='<table><thead><tr><th>Time</th><th>Symbol</th><th>Direction</th><th>Entry</th><th>Exit</th><th>PnL</th><th>Status</th></tr></thead><tbody>'+rows+'</tbody></table>'; }"
                        + "function renderStatus(payload){document.getElementById('status').textContent=JSON.stringify(payload,null,2);}"
                        + "function formatNumber(value){return typeof value==='number'?value.toFixed(5):value==null?'n/a':value;}"
                        + "function escapeHtml(text){if(text==null)return '';return text.toString().replace(/[&<>\"']/g,function(c){return{'&':'&amp;','<':'&lt;','>':'&gt;','\"':'&quot;','\'' :'&#39;'}[c];});}"
                        + "async function refreshData(){try{const [signals,trades,status]=await Promise.all([fetchJson('/api/signals'),fetchJson('/api/trades'),fetchJson('/api/status')]);renderSignals(signals);renderTrades(trades);renderStatus(status);}catch(err){document.getElementById('signals').innerHTML='<p>Error loading signals.</p>';document.getElementById('trades').innerHTML='<p>Error loading trades.</p>';document.getElementById('status').textContent=err.message;}}"
                        + "refreshData();"
                        + "</script></body></html>";
                byte[] response = html.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().add("Content-Type", "text/html; charset=UTF-8");
                exchange.sendResponseHeaders(200, response.length);
                exchange.getResponseBody().write(response);
                exchange.close();
            });

            dashboardServer.createContext("/api/status", exchange -> {
                java.util.Map<String, Object> payload = new HashMap<>();
                payload.put("liveSignals", new ArrayList<>(recentLiveSignals));
                payload.put("recentTrades", new ArrayList<>(recentTradeRecords));
                payload.put("tradeLoggingEnabled", tradeDatabase != null && tradeDatabase.isEnabled());
                byte[] response = new Gson().toJson(payload).getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().add("Content-Type", "application/json; charset=UTF-8");
                exchange.sendResponseHeaders(200, response.length);
                exchange.getResponseBody().write(response);
                exchange.close();
            });

            dashboardServer.createContext("/api/signals", exchange -> {
                byte[] response = new Gson().toJson(new ArrayList<>(recentLiveSignals)).getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().add("Content-Type", "application/json; charset=UTF-8");
                exchange.sendResponseHeaders(200, response.length);
                exchange.getResponseBody().write(response);
                exchange.close();
            });

            dashboardServer.createContext("/api/trades", exchange -> {
                byte[] response = new Gson().toJson(new ArrayList<>(recentTradeRecords)).getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().add("Content-Type", "application/json; charset=UTF-8");
                exchange.sendResponseHeaders(200, response.length);
                exchange.getResponseBody().write(response);
                exchange.close();
            });

            dashboardServer.setExecutor(java.util.concurrent.Executors.newCachedThreadPool());
            dashboardServer.start();
            System.out.println("💻 Dashboard server running on http://localhost:" + port + "/");
        } catch (IOException e) {
            System.out.println("❌ Failed to start dashboard server: " + e.getMessage());
        }
    }

    // ===============================
    // SIGNAL STRENGTH CALCULATOR
    // ===============================
    public static double calculateSignalStrength(java.util.List<Candle> data, int index,
            int pred, double mlConf, double smcConf) {
        if (index < 20 || index >= data.size()) {
            return 0;
        }

        double strength = 0;

        // 1. ML Confidence component (40% weight)
        strength += mlConf * 0.40;

        // 2. SMC Confluence component (30% weight)
        strength += smcConf * 0.30;

        // 3. ICT Confirmation component (10% weight)
        double ictConf = calculateICTConfluence(data, index, pred);
        strength += ictConf * 0.10;

        // 4. Swing confirmation component (8% weight)
        double swingConf = calculateSwingConfluence(data, index, pred);
        strength += swingConf * 0.08;

        // 5. Correlation component (price/volume correlation)
        double corr = calculatePriceVolumeCorrelation(data, index, 20);
        if ((pred == 1 && corr > 0) || (pred == 0 && corr < 0)) {
            strength += 0.10 * Math.min(1.0, Math.abs(corr));
        } else {
            strength -= 0.05 * Math.min(1.0, Math.abs(corr));
        }

        // 5. Trend alignment (15% weight)
        int structure = detectMarketStructure(data, index, 20);
        if ((pred == 1 && structure == 1) || (pred == 0 && structure == -1)) {
            strength += 0.15; // Signal aligns with trend
        }

        // 4. Volatility check (10% weight) - avoid low volatility traps
        double atr = calculateATR(data, index, 14);
        double avgPrice = data.get(index).close;
        double atrPercent = (atr / avgPrice) * 100;

        if (atrPercent > 0.2) { // Adequate volatility
            strength += 0.10;
        } else {
            strength *= 0.8; // Reduce strength in low volatility
        }

        return Math.min(strength, 1.0) * 100; // Return as percentage
    }

    public static double calculatePriceVolumeCorrelation(java.util.List<Candle> data, int index, int lookback) {
        if (index < lookback || lookback < 2) {
            return 0;
        }
        double[] returns = new double[lookback];
        double[] volume = new double[lookback];
        for (int i = 0; i < lookback; i++) {
            Candle prev = data.get(index - lookback + i);
            Candle current = data.get(index - lookback + i + 1);
            returns[i] = current.close - prev.close;
            volume[i] = current.volume;
        }
        return pearsonCorrelation(returns, volume);
    }

    public static double pearsonCorrelation(double[] x, double[] y) {
        if (x == null || y == null || x.length != y.length || x.length < 2) {
            return 0;
        }
        double meanX = 0;
        double meanY = 0;
        int n = x.length;
        for (int i = 0; i < n; i++) {
            meanX += x[i];
            meanY += y[i];
        }
        meanX /= n;
        meanY /= n;

        double sumXY = 0;
        double sumXX = 0;
        double sumYY = 0;
        for (int i = 0; i < n; i++) {
            double dx = x[i] - meanX;
            double dy = y[i] - meanY;
            sumXY += dx * dy;
            sumXX += dx * dx;
            sumYY += dy * dy;
        }
        if (sumXX == 0 || sumYY == 0) {
            return 0;
        }
        return sumXY / Math.sqrt(sumXX * sumYY);
    }

    // ===============================
    // PERFORMANCE METRICS
    // ===============================
    public static class PerformanceMetrics {

        public int totalSignals;
        public int buySignals, sellSignals;
        public int effectiveSignals;
        public int profitableSignals;
        public double effectiveRatio;
        public double avgConfidence;
        public double avgSignalStrength;
        public double avgRiskRewardRatio;
        public double avgRewardAmount;
        public double profitPotentialFactor;
        public double totalRiskExposure;
        public double maxRisk, minRisk;
        public String summary;

        PerformanceMetrics(java.util.List<TradeSignal> signals) {
            totalSignals = signals.size();
            buySignals = (int) signals.stream().filter(s -> s.direction.equals("BUY")).count();
            sellSignals = (int) signals.stream().filter(s -> s.direction.equals("SELL")).count();
            effectiveSignals = (int) signals.stream().filter(Fxausd::isSignalEffective).count();
            profitableSignals = (int) signals.stream().filter(s -> s.rewardAmount > s.riskAmount).count();
            effectiveRatio = totalSignals == 0 ? 0 : (effectiveSignals * 100.0 / totalSignals);

            avgConfidence = signals.stream().mapToDouble(s -> s.mlConfidence).average().orElse(0) * 100;
            avgSignalStrength = signals.stream().mapToDouble(s -> s.signalStrength).average().orElse(0);
            avgRiskRewardRatio = signals.stream().mapToDouble(s -> s.riskRewardRatio).average().orElse(0);
            avgRewardAmount = signals.stream().mapToDouble(s -> s.rewardAmount).average().orElse(0);
            totalRiskExposure = signals.stream().mapToDouble(s -> s.riskAmount).sum();
            maxRisk = signals.stream().mapToDouble(s -> s.riskAmount).max().orElse(0);
            minRisk = signals.stream().mapToDouble(s -> s.riskAmount).min().orElse(0);
            profitPotentialFactor = calculateProfitPotentialFactor(signals);

            summary = String.format(
                    "📊 Total Signals: %d | BUY: %d | SELL: %d | Effective: %d (%.1f%%) | Profitable: %d\n"
                    + "🎯 Avg ML Confidence: %.2f%% | Avg Signal Strength: %.2f%% | Avg R:R: %.2f:1\n"
                    + "📈 Profit Potential: %.2f:1 | Avg Reward: %.2f | Total Risk Exposure: $%.2f",
                    totalSignals, buySignals, sellSignals, effectiveSignals, effectiveRatio, profitableSignals,
                    avgConfidence, avgSignalStrength, avgRiskRewardRatio,
                    profitPotentialFactor, avgRewardAmount, totalRiskExposure
            );
        }
    }

    // ===============================
    // RISK MANAGEMENT SUMMARY
    // ===============================
    public static class RiskManagement {

        public double accountBalance;
        public double riskPerTrade;
        public double totalPositionSize;
        public int maxConcurrentTrades;
        public double recommendedLotSize;
        public String riskSummary;

        RiskManagement(double balance, java.util.List<TradeSignal> signals) {
            accountBalance = balance;
            riskPerTrade = balance * 0.02; // 2% risk per trade
            totalPositionSize = signals.stream().mapToDouble(s -> s.riskAmount).sum();
            maxConcurrentTrades = Math.min(5, (int) (balance / (riskPerTrade * 10)));
            recommendedLotSize = riskPerTrade / 100; // Position size in standard lots

            double exposurePercent = (totalPositionSize / accountBalance) * 100;
            String riskLevel = exposurePercent < 5 ? "✅ LOW"
                    : exposurePercent < 15 ? "⚠️  MEDIUM" : "🔴 HIGH";

            riskSummary = String.format(
                    "💰 Account Balance: $%.2f\n"
                    + "💸 Risk Per Trade: $%.2f (2%%)\n"
                    + "📊 Total Exposure: $%.2f (%.2f%%) [%s]\n"
                    + "🔄 Max Concurrent Trades: %d\n"
                    + "📈 Recommended Lot Size: %.2f",
                    accountBalance, riskPerTrade, totalPositionSize, exposurePercent,
                    riskLevel, maxConcurrentTrades, recommendedLotSize
            );
        }
    }

    // ===============================
    // CSV EXPORT
    // ===============================
    public static void exportSignalsToCSV(java.util.List<TradeSignal> signals, String filename) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(filename))) {
            // Header
            writer.println("Direction,Entry,StopLoss,TakeProfit,MLConfidence,SMCConfluence,SignalStrength,"
                    + "Risk,Reward,RiskReward,Reason");

            // Data rows
            for (TradeSignal signal : signals) {
                writer.printf("%s,%.4f,%.4f,%.4f,%.2f%%,%.2f%%,%.2f%%,%.4f,%.4f,%.2f:1,%s\n",
                        signal.direction, signal.entry, signal.stopLoss, signal.takeProfit,
                        signal.mlConfidence * 100, signal.smcConfluence * 100, signal.signalStrength,
                        signal.riskAmount, signal.rewardAmount, signal.riskRewardRatio,
                        signal.reason);
            }

            System.out.println("✅ Signals exported to: " + filename);
        } catch (IOException e) {
            System.out.println("❌ Error exporting signals: " + e.getMessage());
        }
    }

    public static SMCSignal generateSMCSignal(java.util.List<Candle> data, int index, int pred, double baseConf) {
        if (index < 0 || index >= data.size()) {
            return new SMCSignal("EURUSD", "NEUTRAL", 0, 0, 0, 0, "Invalid index");
        }

        double entry = data.get(index).close;
        double atr = calculateATR(data, index, 14);
        double orderBlock = detectOrderBlock(data, index, 10);
        double ictScore = calculateICTConfluence(data, index, pred);
        double finalConf = calculateSMCConfluence(data, index, pred, baseConf);
        String reason = "ML+ICT Prediction";
        if (ictScore > 0.5) {
            reason = "ML+ICT Confluence";
        }

        double stopLoss, takeProfit;
        String direction = (pred == 1) ? "BUY" : "SELL";

        // Enhanced SL/TP based on SMC order blocks and liquidity
        if (pred == 1) { // BUY
            // Use order block as support for SL if available
            if (orderBlock > 0.7) {
                stopLoss = entry - 1.2 * atr; // Tighter SL at order block
                takeProfit = entry + 3.5 * atr; // Extend TP to liquidity zone
                reason = "BUY @ Order Block + Uptrend Structure";
            } else {
                stopLoss = entry - 1.5 * atr;
                takeProfit = entry + 3.0 * atr;
            }
        } else { // SELL
            if (orderBlock > 0.7) {
                stopLoss = entry + 1.2 * atr;
                takeProfit = entry - 3.5 * atr;
                reason = "SELL @ Order Block + Downtrend Structure";
            } else {
                stopLoss = entry + 1.5 * atr;
                takeProfit = entry - 3.0 * atr;
            }
        }

        return new SMCSignal("EURUSD", direction, entry, stopLoss, takeProfit, finalConf, reason);
    }

    public static SMCSignal generateCRTSignal(java.util.List<Candle> data, int index, int pred, double baseConf) {
        if (index < 10 || index >= data.size()) {
            return new SMCSignal("EURUSD", "NEUTRAL", 0, 0, 0, 0, "Invalid index");
        }

        double entry = data.get(index).close;
        double atr = calculateATR(data, index, 14);
        double ma20 = movingAverage(data, index, 20);
        double ma50 = movingAverage(data, index, 50);
        double structure = detectMarketStructure(data, index, 20);

        double recentLow = Double.MAX_VALUE;
        double recentHigh = Double.MIN_VALUE;
        int lookback = Math.min(5, index);
        for (int i = index - lookback; i < index; i++) {
            recentLow = Math.min(recentLow, data.get(i).low);
            recentHigh = Math.max(recentHigh, data.get(i).high);
        }

        String direction = pred == 1 ? "BUY" : "SELL";
        double stopLoss;
        double takeProfit;
        String reason = "CRT Reversal/Retest Strategy";

        if (pred == 1) {
            stopLoss = Math.min(entry - 1.2 * atr, recentLow - 0.5 * atr);
            takeProfit = entry + 3.0 * atr;
            if (entry > ma20 && entry > ma50) {
                takeProfit = entry + 3.5 * atr;
                reason = "CRT Buy Trend Retest";
            }
            if (structure == 1) {
                stopLoss = Math.max(stopLoss, entry - 1.0 * atr);
                reason = "CRT Buy Trend Continuation";
            }
        } else {
            stopLoss = Math.max(entry + 1.2 * atr, recentHigh + 0.5 * atr);
            takeProfit = entry - 3.0 * atr;
            if (entry < ma20 && entry < ma50) {
                takeProfit = entry - 3.5 * atr;
                reason = "CRT Sell Trend Retest";
            }
            if (structure == -1) {
                stopLoss = Math.min(stopLoss, entry + 1.0 * atr);
                reason = "CRT Sell Trend Continuation";
            }
        }

        double signalBonus = 0;
        if ((pred == 1 && structure == 1) || (pred == 0 && structure == -1)) {
            signalBonus += 0.10;
        }
        if (Math.abs(entry - ma20) / ma20 < 0.01) {
            signalBonus += 0.05;
        }

        double finalConf = Math.min(1.0, baseConf + signalBonus);
        return new SMCSignal("EURUSD", direction, entry, stopLoss, takeProfit, finalConf, reason);
    }

    public static double calculateSwingConfluence(java.util.List<Candle> data, int index, int pred) {
        if (index < 20 || index >= data.size()) {
            return 0;
        }

        int structure = detectMarketStructure(data, index, 20);
        double orderBlock = detectOrderBlock(data, index, 10);
        double liquidity = detectLiquidityZone(data, index, 10);
        double fvg = detectFairValueGap(data, index);
        double levelFlip = detectLevelFlip(data, index, 20);

        double swingScore = 0;
        if ((pred == 1 && structure == 1) || (pred == 0 && structure == -1)) {
            swingScore += 0.25;
        }
        if (orderBlock > 0.7) {
            swingScore += 0.15;
        }
        if (liquidity > 0.65) {
            swingScore += 0.10;
        }
        if (fvg > 0.4) {
            swingScore += 0.10;
        }
        if ((pred == 1 && levelFlip > 0.5) || (pred == 0 && levelFlip < -0.5)) {
            swingScore += 0.20;
        }

        return Math.min(1.0, swingScore);
    }

    public static SMCSignal generateSwingSignal(java.util.List<Candle> data, int index, int pred, double baseConf) {
        if (index < 20 || index >= data.size()) {
            return new SMCSignal("EURUSD", "NEUTRAL", 0, 0, 0, 0, "Invalid index");
        }

        double entry = data.get(index).close;
        double atr = calculateATR(data, index, 14);
        int structure = detectMarketStructure(data, index, 20);
        double orderBlock = detectOrderBlock(data, index, 10);
        double liquidity = detectLiquidityZone(data, index, 10);
        double fvg = detectFairValueGap(data, index);
        double levelFlip = detectLevelFlip(data, index, 20);

        String direction = pred == 1 ? "BUY" : "SELL";
        double stopLoss;
        double takeProfit;
        String reason = "Swing Strategy Combo";

        if (pred == 1) {
            stopLoss = entry - 1.3 * atr;
            takeProfit = entry + 3.2 * atr;
            if (structure == 1) {
                stopLoss = Math.max(stopLoss, entry - 1.1 * atr);
                reason = "Swing Buy Trend";
            }
            if (orderBlock > 0.7) {
                stopLoss = entry - 1.0 * atr;
                takeProfit = entry + 3.5 * atr;
                reason = "Swing Buy + Order Block";
            }
        } else {
            stopLoss = entry + 1.3 * atr;
            takeProfit = entry - 3.2 * atr;
            if (structure == -1) {
                stopLoss = Math.min(stopLoss, entry + 1.1 * atr);
                reason = "Swing Sell Trend";
            }
            if (orderBlock > 0.7) {
                stopLoss = entry + 1.0 * atr;
                takeProfit = entry - 3.5 * atr;
                reason = "Swing Sell + Order Block";
            }
        }

        double swingConf = calculateSwingConfluence(data, index, pred);
        double finalConf = Math.min(1.0, baseConf + swingConf * 0.4 + fvg * 0.1);
        return new SMCSignal("EURUSD", direction, entry, stopLoss, takeProfit, finalConf, reason);
    }

    public static SMCSignal generateCombinedSignal(java.util.List<Candle> data, int index, int pred, double baseConf, boolean useCRT) {
        SMCSignal baseSignal = useCRT
                ? generateCRTSignal(data, index, pred, baseConf)
                : generateSMCSignal(data, index, pred, baseConf);
        SMCSignal swingSignal = generateSwingSignal(data, index, pred, baseConf);

        double entry = baseSignal.entry;
        double stopLoss = pred == 1
                ? Math.max(baseSignal.stopLoss, swingSignal.stopLoss)
                : Math.min(baseSignal.stopLoss, swingSignal.stopLoss);
        double takeProfit = pred == 1
                ? Math.max(baseSignal.takeProfit, swingSignal.takeProfit)
                : Math.min(baseSignal.takeProfit, swingSignal.takeProfit);

        double ictConf = calculateICTConfluence(data, index, pred);
        double swingConf = calculateSwingConfluence(data, index, pred);
        double combinedConf = Math.min(1.0,
                baseConf * 0.4
                + baseSignal.confidence * 0.2
                + swingSignal.confidence * 0.2
                + ictConf * 0.15
                + swingConf * 0.15);

        String reason = String.format("Combo [%s + %s]", baseSignal.smcReason, useCRT ? "CRT" : "SMC");
        return new SMCSignal("EURUSD", baseSignal.direction, entry, stopLoss, takeProfit, combinedConf, reason);
    }

    // ===============================
    // SIGNAL STRENGTH VISUALIZER
    // ===============================
    public static String buildStrengthBar(double strength) {
        int filled = (int) (strength / 10); // 0-10 bars
        StringBuilder bar = new StringBuilder("[");
        for (int i = 0; i < 10; i++) {
            bar.append(i < filled ? "█" : "░");
        }
        bar.append("]");
        return bar.toString();
    }

    public static double simulateTradePips(java.util.List<Candle> data, int signalIndex, SMCSignal signal,
            double spreadPips, double slippagePips, int maxBars) {
        if (signal == null || signal.entry == 0 || signalIndex + 1 >= data.size()) {
            return 0;
        }
        int entryIndex = Math.min(signalIndex + 1, data.size() - 1);
        double entryPrice = data.get(entryIndex).open;
        double spread = convertPipsToPrice(signal.symbol, spreadPips);
        double slippage = convertPipsToPrice(signal.symbol, slippagePips);

        if ("BUY".equals(signal.direction)) {
            entryPrice += spread + slippage;
        } else {
            entryPrice -= spread + slippage;
        }

        double shift = entryPrice - signal.entry;
        double stopLoss = signal.stopLoss + shift;
        double takeProfit = signal.takeProfit + shift;

        for (int j = entryIndex + 1; j < data.size() && j <= entryIndex + maxBars; j++) {
            double high = data.get(j).high;
            double low = data.get(j).low;

            if ("BUY".equals(signal.direction)) {
                if (low <= stopLoss && high >= takeProfit) {
                    return toPips(signal.symbol, stopLoss - entryPrice); // conservative fill
                }
                if (low <= stopLoss) {
                    return toPips(signal.symbol, stopLoss - entryPrice);
                }
                if (high >= takeProfit) {
                    return toPips(signal.symbol, takeProfit - entryPrice);
                }
            } else {
                if (high >= stopLoss && low <= takeProfit) {
                    return toPips(signal.symbol, entryPrice - stopLoss); // conservative fill
                }
                if (high >= stopLoss) {
                    return toPips(signal.symbol, entryPrice - stopLoss);
                }
                if (low <= takeProfit) {
                    return toPips(signal.symbol, entryPrice - takeProfit);
                }
            }
        }

        int exitIndex = Math.min(entryIndex + maxBars, data.size() - 1);
        double exitPrice = data.get(exitIndex).close;
        return "BUY".equals(signal.direction) ? toPips(signal.symbol, exitPrice - entryPrice) : toPips(signal.symbol, entryPrice - exitPrice);
    }

    public static double simulateTradePips(java.util.List<Candle> data, String symbol, int index, int pred,
            double spreadPips, double slippagePips, int maxBars) {
        if (index + 1 >= data.size()) {
            return 0;
        }
        int entryIndex = Math.min(index + 1, data.size() - 1);
        double entry = data.get(entryIndex).open;
        double atr = calculateATR(data, index, 14);
        double spread = convertPipsToPrice(symbol, spreadPips);
        double slippage = convertPipsToPrice(symbol, slippagePips);

        if (pred == 1) {
            entry += spread + slippage;
        } else {
            entry -= spread + slippage;
        }

        double stopLoss = pred == 1 ? entry - 1.5 * atr : entry + 1.5 * atr;
        double takeProfit = pred == 1 ? entry + 3.0 * atr : entry - 3.0 * atr;

        for (int j = entryIndex + 1; j < data.size() && j <= entryIndex + maxBars; j++) {
            double high = data.get(j).high;
            double low = data.get(j).low;

            if (pred == 1) {
                if (low <= stopLoss) {
                    return toPips(symbol, stopLoss - entry);
                }
                if (high >= takeProfit) {
                    return toPips(symbol, takeProfit - entry);
                }
            } else {
                if (high >= stopLoss) {
                    return toPips(symbol, entry - stopLoss);
                }
                if (low <= takeProfit) {
                    return toPips(symbol, entry - takeProfit);
                }
            }
        }

        int exitIndex = Math.min(entryIndex + maxBars, data.size() - 1);
        double exitPrice = data.get(exitIndex).close;
        return pred == 1 ? toPips(symbol, exitPrice - entry) : toPips(symbol, entry - exitPrice);
    }

    public static double calculateSharpe(java.util.List<Double> returns) {
        if (returns.isEmpty()) {
            return 0;
        }
        double mean = returns.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        double variance = returns.stream().mapToDouble(r -> Math.pow(r - mean, 2)).average().orElse(0);
        double std = Math.sqrt(variance);
        return std == 0 ? 0 : mean / std * Math.sqrt(252);
    }

    public static double calculateMaxDrawdown(java.util.List<Double> returns) {
        double peak = 0;
        double trough = 0;
        double running = 0;
        double maxDrawdown = 0;
        for (double r : returns) {
            running += r;
            if (running > peak) {
                peak = running;
                trough = running;
            }
            if (running < trough) {
                trough = running;
                maxDrawdown = Math.max(maxDrawdown, peak - trough);
            }
        }
        return maxDrawdown;
    }

    public static double calculateProfitFactor(java.util.List<Double> returns) {
        double grossWin = 0;
        double grossLoss = 0;
        for (double r : returns) {
            if (r > 0) {
                grossWin += r;
            } else {
                grossLoss += Math.abs(r);
            }
        }
        return grossLoss == 0 ? grossWin : grossWin / grossLoss;
    }

    public static void printFeatureImportances(RandomForestClassifier model) {
        double[] importances = model.getFeatureImportances();
        System.out.println("📌 Feature Importances:");
        for (int i = 0; i < importances.length && i < FEATURE_NAMES.length; i++) {
            System.out.printf("   %s: %.2f%%%n", FEATURE_NAMES[i], importances[i] * 100);
        }
    }

    public static int getAssignedPort() {
        ProcessBuilder processBuilder = new ProcessBuilder();
        if (processBuilder.environment().get("PORT") != null) {
            return Integer.parseInt(processBuilder.environment().get("PORT"));
        }
        return 4567;
    }

    // ============================================================
    // THE QUANTUM FUSION ENGINE (QFE) - Master Decision Engine
    // ============================================================
    public static java.util.List<TradeSignal> generateQuantumFusionSignals(java.util.List<Candle> candles, String symbol, String liveTimeframe) {
        java.util.List<TradeSignal> signals = new ArrayList<>();
        if (candles == null || candles.size() < 200) return signals;

        // --- MASTER ENVIRONMENT FILTER ---
        if (isHighImpactNewsWindow()) {
            System.out.println("🛡️ [QFE Protocol] News window active - suppressing executions for " + symbol);
            return signals;
        }

        int last = candles.size() - 1;
        String regime = detectMarketRegime(candles, last, 30);
        boolean trending = regime.equals("TRENDING");
        boolean ranging = regime.equals("RANGING") || regime.equals("ACCUMULATION") || regime.equals("DISTRIBUTION");

        // Evaluate both directions
        signals.addAll(evaluateFusionForDirection(candles, symbol, liveTimeframe, "BUY", trending, ranging));
        signals.addAll(evaluateFusionForDirection(candles, symbol, liveTimeframe, "SELL", trending, ranging));

        return signals;
    }

    private static java.util.List<TradeSignal> evaluateFusionForDirection(java.util.List<Candle> candles, String symbol, String timeframe, String dir, boolean trending, boolean ranging) {
        java.util.List<TradeSignal> result = new ArrayList<>();
        int last = candles.size() - 1;
        Candle current = candles.get(last);
        double price = current.close;

        // Weights: Q(35), SMC(30), Sniper(15), BO(10), MR(10), Risk(10)
        double qScore = scoreBrain1_QILH(candles, last, dir);
        double smcScore = scoreBrain2_SMC(candles, last, dir);
        double riskScore = scoreBrain6_Risk(symbol);

        double confidence = 0;
        StringBuilder audit = new StringBuilder(String.format("⚛️ [QFE %s] %s | ", dir, symbol));

        if (trending) {
            double sniperScore = scoreBrain3_Sniper(candles, last, dir);
            double breakoutScore = scoreBrain4_Breakout(candles, last, dir);
            confidence = (qScore * 35) + (smcScore * 30) + (sniperScore * 15) + (breakoutScore * 10) + (riskScore * 10);
            audit.append(String.format("Q:%.0f SMC:%.0f SNP:%.0f BO:%.0f R:%.0f", qScore*35, smcScore*30, sniperScore*15, breakoutScore*10, riskScore*10));
        } else {
            // Ranging/Volatile mode - Redistribute Sniper/BO weight (25 pts) to QILH (+10) and SMC (+15)
            double mrScore = scoreBrain5_MeanReversion(candles, last, dir);
            confidence = (qScore * 45) + (smcScore * 45) + (mrScore * 10) + (riskScore * 10);
            audit.append(String.format("Q:%.0f SMC:%.0f MR:%.0f R:%.0f", qScore*45, smcScore*45, mrScore*10, riskScore*10));
        }

        // Only trade if Confidence >= 95
        if (confidence >= 95) {
            double slPips = calculateAdaptiveStopLoss(candles, last, dir, symbol);
            double tpPips = slPips * (trending ? 3.0 : 2.0); // Trending targets higher R:R
            
            // Dynamic Risk based on confidence
            double riskPct = 0.005; // 0.5% fallback
            if (confidence >= 98) riskPct = 0.02;      // 2% Risk
            else if (confidence >= 95) riskPct = 0.01; // 1% Risk

            String reason = String.format("QFE Fusion %s: Confidence %.0f%% [%s]", dir, confidence, trending ? "TREND" : "RANGE");
            TradeSignal sig = new TradeSignal(symbol, dir, price, 0, 0, confidence/100.0, 0.90, confidence, reason, slPips, tpPips);
            
            // Set prices
            double slPrice = dir.equals("BUY") ? price - convertPipsToPrice(symbol, slPips) : price + convertPipsToPrice(symbol, slPips);
            double tpPrice = dir.equals("BUY") ? price + convertPipsToPrice(symbol, tpPips) : price - convertPipsToPrice(symbol, tpPips);
            sig.stopLoss = slPrice;
            sig.takeProfit = tpPrice;
            
            result.add(sig);
            System.out.println(audit.toString() + " -> " + confidence + "% 🔥 [EXECUTE]");
        } else {
            if (confidence > 75) System.out.println(audit.toString() + " -> " + confidence + "% (Req: 95+)");
        }

        return result;
    }

    private static double scoreBrain1_QILH(java.util.List<Candle> candles, int index, String dir) {
        int sweep = detectLiquiditySweepDirection(candles, index);
        double imbalance = calculateVolumeImbalance(candles, index);
        double orderBlock = detectOrderBlock(candles, index, 15);
        double score = 0;
        if (dir.equals("BUY")) {
            if (sweep == 1) score += 0.6;
            if (imbalance > 0.5) score += 0.2;
            if (orderBlock > 0.6) score += 0.2;
        } else {
            if (sweep == -1) score += 0.6;
            if (imbalance < -0.5) score += 0.2;
            if (orderBlock > 0.6) score += 0.2;
        }
        return Math.min(1.0, score);
    }

    private static double scoreBrain2_SMC(java.util.List<Candle> candles, int index, String dir) {
        int bos = detectBOS(candles, index);
        int choch = detectCHoCH(candles, index);
        double fvg = detectFairValueGap(candles, index);
        double score = 0;
        if (dir.equals("BUY")) {
            if (bos == 1 || choch == 1) score += 0.7;
            if (fvg > 0.3) score += 0.3;
        } else {
            if (bos == -1 || choch == -1) score += 0.7;
            if (fvg < -0.3) score += 0.3;
        }
        return Math.min(1.0, score);
    }

    private static double scoreBrain3_Sniper(java.util.List<Candle> candles, int index, String dir) {
        double atr = calculateATR(candles, index, 14);
        double body = Math.abs(candles.get(index).close - candles.get(index).open);
        double avgVol = calculateAverageVolume(candles, index - 1, 20);
        double currentVol = candles.get(index).volume;
        
        double score = 0;
        if (currentVol > avgVol * 1.3) score += 0.5;
        if (body > atr * 0.8) score += 0.5;
        
        // Directional alignment
        if (dir.equals("BUY") && candles.get(index).close < candles.get(index).open) score *= 0.5;
        if (dir.equals("SELL") && candles.get(index).close > candles.get(index).open) score *= 0.5;
        
        return score;
    }

    private static double scoreBrain4_Breakout(java.util.List<Candle> candles, int index, String dir) {
        double recentHigh = getRecentHigh(candles, index - 15, index - 1);
        double recentLow = getRecentLow(candles, index - 15, index - 1);
        double price = candles.get(index).close;
        double atr = calculateATR(candles, index, 14);
        
        if (dir.equals("BUY") && price > recentHigh + (atr * 0.2)) return 1.0;
        if (dir.equals("SELL") && price < recentLow - (atr * 0.2)) return 1.0;
        return 0.0;
    }

    private static double scoreBrain5_MeanReversion(java.util.List<Candle> candles, int index, String dir) {
        double rsi = calculateRSI(candles, index, 14);
        double score = 0;
        if (dir.equals("BUY")) {
            if (rsi < 35) score += 0.8;
            if (rsi < 30) score += 0.2;
        } else {
            if (rsi > 65) score += 0.8;
            if (rsi > 70) score += 0.2;
        }
        return score;
    }

    private static double scoreBrain6_Risk(String symbol) {
        double spread = getCurrentSpreadPips(symbol);
        if (spread <= getMaxAllowedSpread(symbol) * 0.8) return 1.0;
        if (spread <= getMaxAllowedSpread(symbol)) return 0.7;
        return 0.0;
    }

    private static double getMaxAllowedSpread(String symbol) {
        if (symbol == null) return 3.0;
        String s = symbol.toUpperCase();
        if (s.contains("XAU") || s.contains("GOLD")) return 35.0;
        if (s.contains("EURUSD")) return 1.5;
        if (s.contains("GBPUSD")) return 2.0;
        if (isCrypto(symbol)) return 150.0;
        return 3.0;
    }

    private static double calculateAdaptiveStopLoss(java.util.List<Candle> candles, int index, String dir, String symbol) {
        double atr = calculateATR(candles, index, 14);
        double minSl = isGoldSymbol(symbol) ? 25.0 : 8.0;
        double baseSl = convertPriceDiffToPips(symbol, atr * 1.8);
        return Math.max(minSl, baseSl);
    }

}

