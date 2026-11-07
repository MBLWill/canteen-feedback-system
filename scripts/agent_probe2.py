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
            "description": "根据关键词模糊搜索食堂菜品，返回菜名、所属档口、价格、评分、好评率、评价数。",
            "parameters": {
                "type": "object",
                "properties": {
                    "keyword": {"type": "string", "description": "菜品名关键词"},
                    "limit": {"type": "integer", "description": "返回数量上限，默认 5"}
                },
                "required": ["keyword"]
            }
        }
    }
]


def chat(messages, tools=None, stream=False, timeout=150):
    body = {"model": MODEL, "messages": messages, "stream": stream, "temperature": 0.3}
    if tools:
        body["tools"] = tools
    data = json.dumps(body).encode("utf-8")
    req = urllib.request.Request(API, data=data, method="POST")
    req.add_header("Authorization", "Bearer " + KEY)
    req.add_header("Content-Type", "application/json")
    try:
        resp = urllib.request.urlopen(req, timeout=timeout)
    except urllib.error.HTTPError as e:
        return {"error": e.code, "detail": e.read().decode("utf-8", "ignore")[:800]}
    except Exception as e:
        return {"error": "exception", "detail": str(e)[:300]}
    if stream:
        return resp
    return json.loads(resp.read().decode("utf-8"))


def test_toolcall_long():
    print("=== A) 工具调用（timeout=150，不带 tool_choice）===")
    msgs = [{"role": "user", "content": "宫保鸡丁多少钱？在哪个档口？"}]
    r = chat(msgs, tools=TOOLS, timeout=150)
    print(json.dumps(r, ensure_ascii=False)[:1200])


def test_stream_basic():
    print("\n=== B) 流式基础对话 ===")
    resp = chat([{"role": "user", "content": "用10个字介绍宫保鸡丁"}], stream=True, timeout=60)
    if isinstance(resp, dict):
        print("ERR", resp); return
    full = []
    for raw in resp:
        line = raw.decode("utf-8").strip()
        if not line.startswith("data:"):
            continue
        p = line[5:].strip()
        if p == "[DONE]":
            break
        try:
            o = json.loads(p)
        except Exception:
            continue
        c = o["choices"][0].get("delta", {}).get("content")
        if c:
            full.append(c)
    print("流式输出:", "".join(full))


def test_stream_tool():
    print("\n=== C) 流式 + 工具调用（看 delta.tool_calls 片段）===")
    msgs = [{"role": "user", "content": "查一下热门菜有哪些"}]
    resp = chat(msgs, tools=TOOLS, stream=True, timeout=150)
    if isinstance(resp, dict):
        print("ERR", resp); return
    txt = []
    tc_chunks = []
    for raw in resp:
        line = raw.decode("utf-8").strip()
        if not line.startswith("data:"):
            continue
        p = line[5:].strip()
        if p == "[DONE]":
            break
        try:
            o = json.loads(p)
        except Exception:
            continue
        d = o["choices"][0].get("delta", {})
        if d.get("content"):
            txt.append(d["content"])
        if d.get("tool_calls"):
            tc_chunks.append(d["tool_calls"])
    print("content片段:", "".join(txt)[:200])
    print("tool_calls片段数:", len(tc_chunks))
    print("tool_calls样例:", json.dumps(tc_chunks[0] if tc_chunks else None, ensure_ascii=False)[:600])


if __name__ == "__main__":
    if not KEY:
        print("缺少 AGENT_KEY"); sys.exit(1)
    test_toolcall_long()
    test_stream_basic()
    test_stream_tool()
