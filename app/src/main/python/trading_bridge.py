import json
import os
import traceback

KEY_VARS = {
    "openai": "OPENAI_API_KEY",
    "anthropic": "ANTHROPIC_API_KEY",
    "google": "GOOGLE_API_KEY",
    "deepseek": "DEEPSEEK_API_KEY",
    "openrouter": "OPENROUTER_API_KEY",
    "groq": "GROQ_API_KEY",
}


def analyze(ticker, date, provider, deep_model, quick_model, api_key, data_dir):
    try:
        os.environ["TRADINGAGENTS_MEMORY_LOG_PATH"] = os.path.join(data_dir, "memory.md")
        os.environ["TRADINGAGENTS_CACHE_DIR"] = os.path.join(data_dir, "cache")

        env_var = KEY_VARS.get(provider)
        if env_var and api_key:
            os.environ[env_var] = api_key

        from tradingagents.graph.trading_graph import TradingAgentsGraph
        from tradingagents.default_config import DEFAULT_CONFIG

        config = DEFAULT_CONFIG.copy()
        config["llm_provider"] = provider
        config["deep_think_llm"] = deep_model
        config["quick_think_llm"] = quick_model
        config["max_debate_rounds"] = 1
        config["max_risk_discuss_rounds"] = 1

        ta = TradingAgentsGraph(debug=False, config=config)
        _, decision = ta.propagate(ticker.upper(), date)

        return json.dumps(decision, indent=2, default=str)
    except Exception:
        return json.dumps({"error": traceback.format_exc()}, indent=2)
