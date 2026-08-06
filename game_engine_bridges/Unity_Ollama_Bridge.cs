using System.Collections;
using System.Text;
using UnityEngine;
using UnityEngine.Networking;
using System;

[Serializable]
public class OllamaRequest {
    public string model;
    public string prompt;
    public bool stream;
}

[Serializable]
public class OllamaResponse {
    public string model;
    public string response;
    public bool done;
}

public class OllamaSwarmBridge : MonoBehaviour
{
    private const string OLLAMA_URL = "http://localhost:11434/api/generate";

    /// <summary>
    /// Sends a prompt to the local Ollama Swarm Node and executes a callback with the response.
    /// </summary>
    public void GenerateText(string modelName, string systemPrompt, Action<string> onComplete)
    {
        StartCoroutine(PostRequest(modelName, systemPrompt, onComplete));
    }

    private IEnumerator PostRequest(string modelName, string prompt, Action<string> onComplete)
    {
        OllamaRequest reqData = new OllamaRequest {
            model = modelName,
            prompt = prompt,
            stream = false
        };

        string jsonPayload = JsonUtility.ToJson(reqData);
        
        using (UnityWebRequest request = new UnityWebRequest(OLLAMA_URL, "POST"))
        {
            byte[] bodyRaw = Encoding.UTF8.GetBytes(jsonPayload);
            request.uploadHandler = new UploadHandlerRaw(bodyRaw);
            request.downloadHandler = new DownloadHandlerBuffer();
            request.SetRequestHeader("Content-Type", "application/json");

            yield return request.SendWebRequest();

            if (request.result == UnityWebRequest.Result.ConnectionError || request.result == UnityWebRequest.Result.ProtocolError)
            {
                Debug.LogError($"[OLLAMA BRIDGE ERROR] {request.error}");
                onComplete?.Invoke("[ERROR] Failed to contact local Swarm.");
            }
            else
            {
                string jsonResponse = request.downloadHandler.text;
                OllamaResponse res = JsonUtility.FromJson<OllamaResponse>(jsonResponse);
                onComplete?.Invoke(res.response);
            }
        }
    }
}
