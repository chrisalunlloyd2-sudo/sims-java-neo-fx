import unreal
import urllib.request
import urllib.error
import json

@unreal.uclass()
class OllamaSwarmBridge(unreal.BlueprintFunctionLibrary):
    """
    Unreal Engine Python Bridge for local SIMS1337 Swarm integration.
    Requires the 'Python Editor Script Plugin' enabled in UE.
    """

    @unreal.ufunction(ret=unreal.Text, params=[unreal.Text, unreal.Text], static=True, meta=dict(Category="AI Swarm"))
    def generate_swarm_text(model_name, prompt):
        url = "http://localhost:11434/api/generate"
        
        data = {
            "model": str(model_name),
            "prompt": str(prompt),
            "stream": False
        }
        
        payload = json.dumps(data).encode('utf-8')
        req = urllib.request.Request(url, data=payload, headers={'Content-Type': 'application/json'}, method='POST')
        
        try:
            with urllib.request.urlopen(req) as response:
                result_bytes = response.read()
                result_str = result_bytes.decode('utf-8')
                result_json = json.loads(result_str)
                return result_json.get("response", "Error: No response key.")
        except urllib.error.URLError as e:
            unreal.log_error(f"[OLLAMA BRIDGE ERROR] {e.reason}")
            return f"Error: {e.reason}"
