//+------------------------------------------------------------------+
//| ForexBot MT5 - QFE Fusion Monitor                              |
//+------------------------------------------------------------------+
#property copyright "Copyright 2026, FXAUSD"
#property link      "https://fxausd.onrender.com"
#property version   "2.00"
#property strict

// --- Monitor Configuration ---
input string BotStatus = "CONNECTED TO JAVA QFE";
input bool ShowTerminalInfo = true;

//+------------------------------------------------------------------+
//| Expert initialization function                                   |
//+------------------------------------------------------------------+
int OnInit()
{
    // Set a timer to update the dashboard every second
    EventSetTimer(1);

    Print("🚀 QFE Monitor Initialized. Python Bridge is now handling executions.");

    // Initial UI Update
    UpdateDashboard();

    return(INIT_SUCCEEDED);
}

//+------------------------------------------------------------------+
//| Expert deinitialization function                                 |
//+------------------------------------------------------------------+
void OnDeinit(const int reason)
{
    EventKillTimer();
    Comment(""); // Clear chart comment
}

//+------------------------------------------------------------------+
//| Timer function                                                   |
//+------------------------------------------------------------------+
void OnTimer()
{
    UpdateDashboard();
}

//+------------------------------------------------------------------+
//| Update the chart dashboard                                       |
//+------------------------------------------------------------------+
void UpdateDashboard()
{
    string tradeStatus = TerminalInfoInteger(TERMINAL_TRADE_ALLOWED) ? "ENABLED ✅" : "DISABLED ❌ (Enable Algo Trading)";
    string bridgeStatus = "ACTIVE (PORT 5005)";
    
    string display = "\n\n" +
                     "  FXAUSD QUANTUM FUSION ENGINE\n" +
                     "  ---------------------------------\n" +
                     "  SYSTEM STATUS : " + BotStatus + "\n" +
                     "  MT5 BRIDGE    : " + bridgeStatus + "\n" +
                     "  ALGO TRADING  : " + tradeStatus + "\n" +
                     "  SERVER TIME   : " + TimeToString(TimeCurrent(), TIME_SECONDS) + "\n" +
                     "  ACCOUNT       : " + (string)AccountInfoInteger(ACCOUNT_LOGIN) + "\n" +
                     "  ---------------------------------\n" +
                     "  Waiting for Institutional Fusion Consensus...";

    Comment(display);
}

//+------------------------------------------------------------------+
//| Expert tick function                                             |
//+------------------------------------------------------------------+
void OnTick()
{
    // Execution is now handled by the high-speed Python Bridge.
    // This EA provides visual status and terminal heartbeat.
}
