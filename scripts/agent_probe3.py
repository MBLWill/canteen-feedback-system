import json
import os
import urllib.request

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


def stream(messages, tools):
    body = {"model": MODEL, "messages": messages, "stream": True, "temperature": 0.3, "tools": tools}
    req = urllib.request.Request(API, data=json.dumps(body).encode(), method="POST")
    req.add_header("Authorization", "Bearer " + KEY)
    req.add_header("Content-Type", "application/json")
    resp = urllib.request.urlopen(req, timeout=150)
    content = []
    tc = []
    for raw in resp:
        line = raw.decode().strip()
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
            content.append(d["content"])
        if d.get("tool_calls"):
            tc.append(d["tool_calls"])
    return "".join(content), tc


if __name__ == "__main__":
    print("=== 流式工具调用片段（问：宫保鸡丁多少钱）===")
    c, tc = stream([{"role": "user", "content": "宫保鸡丁多少钱？在哪个档口？"}], TOOLS)
    print("content:", repr(c))
    print("tool_calls 片段数:", len(tc))
    for i, frag in enumerate(tc):
        print(f"  片段[{i}]:", json.dumps(frag, ensure_ascii=False))
