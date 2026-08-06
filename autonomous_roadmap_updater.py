import re
import subprocess
import requests

def query_ollama(model_name, prompt):
    try:
        res = requests.post(
            "http://localhost:11434/api/generate",
            json={"model": model_name, "prompt": prompt, "stream": False},
            timeout=120
        )
        if res.status_code == 200:
            return res.json().get("response", "")
    except:
        pass
    return "Error querying Ollama."

def append_new_step_to_readme():
    print("Generating new roadmap step using neuromorphic history...")
    prompt = "Look at the history of neuromorphic computing. Propose EXACTLY ONE single sentence step for a local AI agent swarm to improve its code review, troubleshooting, or terminal driving. Start the sentence with a verb. Do not include markdown or numbers."
    new_step = query_ollama("deepseek-r1:1.5b", prompt).strip()
    # Clean up reasoning tokens if deepseek outputs them
    new_step = re.sub(r'<think>.*?</think>', '', new_step, flags=re.DOTALL).strip()
    
    if not new_step or "Error" in new_step:
        print("Failed to generate step.")
        return

    print(f"Proposed Step: {new_step}")
    
    with open("README.md", "r", encoding="utf-8") as f:
        readme = f.read()

    # Append to Phase 7
    if "### 🧠 Phase 7: Autonomous Self-Evolution" not in readme:
        readme += "\n\n### 🧠 Phase 7: Autonomous Self-Evolution\n"
    
    # Calculate the new step number
    step_count = len(re.findall(r'- \[[ x]\] \d+\.', readme)) + 1
    new_line = f"- [ ] {step_count}. {new_step}\n"
    
    # Insert at the end of Phase 7 or end of file
    readme += new_line

    with open("README.md", "w", encoding="utf-8") as f:
        f.write(readme)

    print("Added new step to README.md. Committing to GitHub...")
    subprocess.run(["git", "add", "README.md"])
    subprocess.run(["git", "commit", "-m", f"docs: Autonomous Roadmap Evolution - Added step {step_count}"])
    subprocess.run(["git", "push"])

def create_remote_repo(name):
    try:
        with open(r"C:\Users\viper\.git-credentials", "r") as f:
            cred = f.read().strip()
            pat = cred.split(":")[2].split("@")[0]
    except:
        pat = ""
    headers = {"Authorization": f"Bearer {pat}", "Accept": "application/vnd.github.v3+json"}
    res = requests.post("https://api.github.com/user/repos", json={"name": name, "private": False}, headers=headers)
    print(f"[GITHUB REPO CREATE] {name}: Code {res.status_code}")
    return res.status_code

if __name__ == "__main__":
    create_remote_repo("sims-java-neo-fx")
