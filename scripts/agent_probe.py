import json
import os
import sys
import urllib.request
import urllib.error

API = "https://api.siliconflow.cn/v1/chat/completions"
KEY = os.environ.get("AGENT_KEY", "")
MODEL = "Qwen/Qwen2.5-7B-Instruct"

TOOLS = [
    {
        "type": "function",
        "function": {
            "name": "search_dishes",
            "description": "根据关键词模糊搜索食堂菜品，返回菜名、所属档口、价格、评分、好评率、评价数。用于用户问某道菜的价格、评分、在哪个档口。",
            "parameters": {
                "type": "object",
                "properties": {
                    "keyword": {"type": "string", "description": "菜品名关键词，例如 宫保鸡丁"},
                    "limit": {"type": "integer", "description": "返回数量上限，默认 5"}
                },
                "required": ["keyword"]
            }
        }
    },
    {
        "type": "function",
        "function": {
            "name": "get_rankings",
            "description": "返回榜单：hot=热门好评菜, avoid=避雷菜, new=新菜。",
            "parameters": {
                "type": "object",
                "properties": {
                    "type": {"type": "string", "enum": ["hot", "avoid", "new"], "description": "榜单类型"}
                },
                "required": ["type"]
            }
        }
    }
]


def chat(messages, tools=None, stream=False, timeout=60):
    body = {"model": MODEL, "messages": messages, "stream": stream, "temperature": 0.3}
    if tools:
        body["tools"] = tools
        body["tool_choice"] = "auto"
    data = json.dumps(body).encode("utf-8")
    req = urllib.request.Request(API, data=data, method="POST")
    req.add_header("Authorization", "Bearer " + KEY)
    req.add_header("Content-Type", "application/json")
    try:
        resp = urllib.request.urlopen(req, timeout=timeout)
    except urllib.error.HTTPError as e:
        return {"error": e.code, "detail": e.read().decode("utf-8", "ignore")[:500]}
    if stream:
        return resp
    return json.loads(resp.read().decode("utf-8"))


def test_basic():
    print("=== 1) 普通对话 ===")
    r = chat([{"role": "user", "content": "用一句话介绍你自己"}])
    if "error" in r:
        print("ERR", r); return
    print("model:", r.get("model"))
    print("usage:", r.get("usage"))
    print("reply:", r["choices"][0]["message"]["content"][:200])


def test_toolcall():
    print("\n=== 2) 工具调用(function calling) ===")
    msgs = [{"role": "user", "content": "宫保鸡丁多少钱？在哪个档口？"}]
    r = chat(msgs, tools=TOOLS)
    if "error" in r:
        print("ERR", r); return
    msg = r["choices"][0]["message"]
    print("finish_reason:", r["choices"][0].get("finish_reason"))
    print("content:", repr(msg.get("content")))
    print("tool_calls:", json.dumps(msg.get("tool_calls"), ensure_ascii=False))


def test_stream_and_loop():
    print("\n=== 3) 流式 + 工具循环（模拟 AgentService 逻辑）===")
    msgs = [{"role": "user", "content": "今天有什么菜值得推荐？帮我查查热门菜"}]
    r = chat(msgs, tools=TOOLS)
    if "error" in r:
        print("ERR", r); return
    assist = r["choices"][0]["message"]
    print("第一轮 finish_reason:", r["choices"][0].get("finish_reason"))
    print("第一轮 tool_calls:", json.dumps(assist.get("tool_calls"), ensure_ascii=False))
    if not assist.get("tool_calls"):
        print("模型未触发工具，跳过循环演示"); return
    tool_results = []
    for tc in assist["tool_calls"]:
        fn = tc["function"]
        args = json.loads(fn.get("arguments") or "{}")
        fake = "[{\"dishName\":\"宫保鸡丁\",\"stall\":\"川味小炒(05号窗口)\",\"price\":12.0,\"avgRating\":4.45,\"goodRate\":92,\"reviewCount\":120}]"
        tool_results.append({"tool_call_id": tc["id"], "role": "tool",
                             "name": fn["name"], "content": fake})
    msgs.append(assist)
    msgs.extend(tool_results)
    print("--- 第二轮流式输出 ---")
    resp = chat(msgs, stream=True, timeout=60)
    if isinstance(resp, dict) and "error" in resp:
        print("ERR", resp); return
    full = []
    for raw in resp:
        line = raw.decode("utf-8").strip()
        if not line or not line.startswith("data:"):
            continue
        payload = line[len("data:"):].strip()
        if payload == "[DONE]":
            break
        try:
            obj = json.loads(payload)
        except Exception:
            continue
        delta = obj["choices"][0].get("delta", {})
        c = delta.get("content")
        if c:
            full.append(c)
            sys.stdout.write(c); sys.stdout.flush()
    print("\n[第二轮流式完成，共 %d 字符]" % len("".join(full)))


if __name__ == "__main__":
    if not KEY:
        print("缺少环境变量 AGENT_KEY"); sys.exit(1)
    test_basic()
    test_toolcall()
    test_stream_and_loop()
