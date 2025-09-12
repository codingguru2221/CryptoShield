#!/usr/bin/env python3
"""
Password Manager Local Server
Serves password data from pendrive to browser extension via localhost
"""

from flask import Flask, jsonify, request, send_from_directory
from flask_cors import CORS
import json
import os
import base64
import threading
import time
from pathlib import Path
from time import time as unix_time

app = Flask(__name__)
CORS(app)  # Enable CORS for browser extension

# Global variables
pendrive_path = None
passwords_data = []
server_running = False
last_java_heartbeat_ts = 0.0  # epoch seconds; updated by Java app

class PasswordManager:
    def __init__(self):
        self.passwords_file = "passwords.txt"
        self.pendrive_root = None
    
    def set_pendrive_path(self, path):
        """Set the pendrive path where passwords are stored"""
        self.pendrive_root = Path(path)
        print(f"Pendrive path set to: {self.pendrive_root}")
    
    def load_passwords(self):
        """Load passwords from the pendrive"""
        if not self.pendrive_root:
            return []
        
        passwords_file = self.pendrive_root / self.passwords_file
        passwords = []
        
        try:
            if passwords_file.exists():
                with open(passwords_file, 'r', encoding='utf-8') as f:
                    for line in f:
                        line = line.strip()
                        if line:
                            try:
                                # Decode Base64 password data
                                decoded = base64.b64decode(line).decode('utf-8')
                                parts = decoded.split(',')
                                if len(parts) >= 3:
                                    passwords.append({
                                        'website': parts[0],
                                        'username': parts[1],
                                        'password': parts[2],
                                        'id': len(passwords)
                                    })
                            except Exception as e:
                                print(f"Error decoding password line: {e}")
                                continue
        except Exception as e:
            print(f"Error loading passwords: {e}")
        
        return passwords
    
    def save_password(self, website, username, password):
        """Save a new password to the pendrive"""
        if not self.pendrive_root:
            return False
        
        try:
            # Encode password data
            data = f"{website},{username},{password}"
            encoded = base64.b64encode(data.encode('utf-8')).decode('utf-8')
            
            passwords_file = self.pendrive_root / self.passwords_file
            with open(passwords_file, 'a', encoding='utf-8') as f:
                f.write(encoded + '\n')
            
            return True
        except Exception as e:
            print(f"Error saving password: {e}")
            return False
    
    def get_passwords_for_website(self, website):
        """Get passwords for a specific website"""
        passwords = self.load_passwords()
        return [p for p in passwords if website.lower() in p['website'].lower()]

# Global password manager instance
password_manager = PasswordManager()

@app.route('/')
def index():
    """Serve the main page"""
    return jsonify({
        'status': 'Password Manager Server Running',
        'pendrive_detected': pendrive_path is not None,
        'pendrive_path': pendrive_path
    })

@app.route('/api/status')
def get_status():
    """Get server status"""
    java_running = (unix_time() - last_java_heartbeat_ts) < 6.0 if last_java_heartbeat_ts else False
    return jsonify({
        'server_running': True,
        'java_running': java_running,
        'pendrive_detected': pendrive_path is not None,
        'pendrive_path': pendrive_path,
        'passwords_count': len(password_manager.load_passwords())
    })

@app.route('/api/passwords')
def get_passwords():
    """Get all passwords"""
    passwords = password_manager.load_passwords()
    return jsonify({
        'passwords': passwords,
        'count': len(passwords)
    })

@app.route('/api/passwords/<website>')
def get_passwords_for_website(website):
    """Get passwords for a specific website"""
    passwords = password_manager.get_passwords_for_website(website)
    return jsonify({
        'website': website,
        'passwords': passwords,
        'count': len(passwords)
    })

@app.route('/api/save', methods=['POST'])
def save_password():
    """Save a new password"""
    data = request.get_json()
    
    if not data or not all(key in data for key in ['website', 'username', 'password']):
        return jsonify({'error': 'Missing required fields'}), 400
    
    success = password_manager.save_password(
        data['website'],
        data['username'],
        data['password']
    )
    
    if success:
        return jsonify({'status': 'Password saved successfully'})
    else:
        return jsonify({'error': 'Failed to save password'}), 500

@app.route('/api/search', methods=['POST'])
def search_passwords():
    """Search passwords by website or username"""
    data = request.get_json()
    query = data.get('query', '').lower()
    
    if not query:
        return jsonify({'passwords': [], 'count': 0})
    
    passwords = password_manager.load_passwords()
    filtered = [
        p for p in passwords 
        if query in p['website'].lower() or query in p['username'].lower()
    ]
    
    return jsonify({
        'passwords': filtered,
        'count': len(filtered),
        'query': query
    })

@app.route('/api/heartbeat', methods=['POST'])
def heartbeat():
    """Receive heartbeat from the Java application to confirm it is running.
    The Java app should POST here every ~2 seconds while active.
    """
    global last_java_heartbeat_ts
    last_java_heartbeat_ts = unix_time()
    return jsonify({'status': 'ok', 'ts': last_java_heartbeat_ts})

def start_server(port=5000, pendrive_path_param=None):
    """Start the Flask server"""
    global pendrive_path, server_running
    
    if pendrive_path_param:
        pendrive_path = pendrive_path_param
        password_manager.set_pendrive_path(pendrive_path)
    
    server_running = True
    print(f"Starting Password Manager Server on port {port}")
    print(f"Pendrive path: {pendrive_path}")
    
    app.run(host='127.0.0.1', port=port, debug=False, use_reloader=False)

def stop_server():
    """Stop the server"""
    global server_running
    server_running = False
    print("Stopping Password Manager Server")

if __name__ == '__main__':
    import sys
    port = 5000
    pendrive_path_arg = None
    
    if len(sys.argv) > 1:
        pendrive_path_arg = sys.argv[1]
    if len(sys.argv) > 2:
        port = int(sys.argv[2])
    
    start_server(port, pendrive_path_arg)
