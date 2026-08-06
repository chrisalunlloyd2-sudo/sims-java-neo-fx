extends Node
class_name OllamaSwarmBridge

const OLLAMA_URL = "http://localhost:11434/api/generate"

signal response_received(response_text)
signal error_occurred(error_message)

var http_request : HTTPRequest

func _ready():
    http_request = HTTPRequest.new()
    add_child(http_request)
    http_request.connect("request_completed", self, "_on_request_completed")

# Call this from any Godot Node to ping the local SLM Swarm
func generate_text(model_name: String, prompt: String):
    var headers = ["Content-Type: application/json"]
    var payload = {
        "model": model_name,
        "prompt": prompt,
        "stream": false
    }
    
    var json_payload = JSON.print(payload)
    var err = http_request.request(OLLAMA_URL, headers, true, HTTPClient.METHOD_POST, json_payload)
    
    if err != OK:
        emit_signal("error_occurred", "Failed to construct HTTP Request.")

func _on_request_completed(result, response_code, headers, body):
    if response_code == 200:
        var response_body = body.get_string_from_utf8()
        var json = JSON.parse(response_body)
        
        if json.error == OK:
            var data = json.result
            if data.has("response"):
                emit_signal("response_received", data["response"])
            else:
                emit_signal("error_occurred", "Malformed response from Ollama.")
        else:
            emit_signal("error_occurred", "Failed to parse JSON.")
    else:
        emit_signal("error_occurred", "HTTP Error: " + str(response_code))
