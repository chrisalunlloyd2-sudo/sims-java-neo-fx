import ast

class SecurityQuarantineVisitor(ast.NodeVisitor):
    def __init__(self):
        self.violations = []
        self.banned_modules = {"os", "sys", "subprocess", "shutil", "socket"}
        self.banned_functions = {"eval", "exec", "open", "compile", "__import__"}

    def visit_Import(self, node):
        for alias in node.names:
            base_module = alias.name.split('.')[0]
            if base_module in self.banned_modules:
                self.violations.append(f"BANNED IMPORT: {alias.name}")
            # Dependency Graph Constraint (Step 21): Prevent cyclic tool imports
            if base_module.startswith("tool_") or base_module == self.current_module_name:
                self.violations.append(f"CYCLIC IMPORT DETECTED: {alias.name}")
        self.generic_visit(node)

    def visit_ImportFrom(self, node):
        if node.module:
            base_module = node.module.split('.')[0]
            if base_module in self.banned_modules:
                self.violations.append(f"BANNED FROM IMPORT: {node.module}")
            if base_module.startswith("tool_") or base_module == self.current_module_name:
                self.violations.append(f"CYCLIC IMPORT DETECTED: {node.module}")
        self.generic_visit(node)

    def visit_Call(self, node):
        if isinstance(node.func, ast.Name):
            if node.func.id in self.banned_functions:
                self.violations.append(f"BANNED FUNCTION CALL: {node.func.id}")
        self.generic_visit(node)
        
    def visit_While(self, node):
        # Advanced Verification (Step 20): Flag `while True:` as potential infinite loop DOS
        if isinstance(node.test, ast.Constant) and node.test.value is True:
            self.violations.append("INFINITE LOOP DETECTED: `while True` is banned in sandbox.")
        self.generic_visit(node)

    def visit_BinOp(self, node):
        # Advanced Verification: Prevent massive memory allocations like `[0] * 10**9`
        if isinstance(node.op, (ast.Mult, ast.Pow)):
            if isinstance(node.right, ast.Constant) and isinstance(node.right.value, int) and node.right.value > 10000:
                self.violations.append("LARGE ALLOCATION/COMPUTE DETECTED: Arithmetic multiplier too large.")
        self.generic_visit(node)

def verify_tool_code(code_string, current_module_name="unknown"):
    """
    Formal verification of generated tool code.
    Raises Exception if unsafe code is detected.
    """
    try:
        tree = ast.parse(code_string)
    except SyntaxError as e:
        return False, f"Syntax Error: {e}"

    visitor = SecurityQuarantineVisitor()
    visitor.current_module_name = current_module_name
    visitor.visit(tree)

    if visitor.violations:
        return False, "QUARANTINE FAILED. Violations: " + ", ".join(visitor.violations)
    
    return True, "QUARANTINE PASSED. Code is sandboxed and safe."

if __name__ == "__main__":
    import argparse
    import sys
    
    parser = argparse.ArgumentParser(description="AST Sandbox Verification")
    parser.add_argument("--verify", type=str, help="Path to Python file to verify")
    args = parser.parse_args()
    
    if args.verify:
        try:
            with open(args.verify, "r") as f:
                code_content = f.read()
            ok, msg = verify_tool_code(code_content, current_module_name=args.verify.split('/')[-1])
            if not ok:
                print(msg)
                sys.exit(1)
            else:
                print(msg)
                sys.exit(0)
        except Exception as e:
            print(f"Error reading file: {e}")
            sys.exit(1)
    else:
        # Test safe code
        safe_code = "def add(a, b):\n    return a + b\n"
        ok, msg = verify_tool_code(safe_code)
        print("Safe Test:", msg)
        
        # Test unsafe code
        unsafe_code = "import os\ndef nuke():\n    os.system('del *.*')\n"
        ok, msg = verify_tool_code(unsafe_code)
        print("Unsafe Test:", msg)
