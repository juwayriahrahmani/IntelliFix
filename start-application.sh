#!/bin/bash

echo "🚀 Starting Log Analyzer Application Setup..."

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to print colored output
print_status() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Check if running in the correct directory
if [[ ! -d "backend" || ! -d "frontend" ]]; then
    print_error "Please run this script from the log-analyzer-app directory"
    exit 1
fi

print_status "Setting up Log Analyzer Application..."

# Backend Setup
print_status "Starting Backend Setup..."

cd backend

# Check if Maven is installed
if ! command -v mvn &> /dev/null; then
    print_warning "Maven not found. Installing Maven..."
    sudo apt-get update
    sudo apt-get install -y maven openjdk-17-jdk
fi

# Build and start backend
print_status "Building Spring Boot backend..."
mvn clean install -DskipTests

if [ $? -eq 0 ]; then
    print_success "Backend build completed successfully!"
    
    print_status "Starting Spring Boot application..."
    # Start backend in background
    nohup mvn spring-boot:run > ../backend.log 2>&1 &
    BACKEND_PID=$!
    echo $BACKEND_PID > ../backend.pid
    
    print_success "Backend started with PID: $BACKEND_PID"
    print_status "Backend logs are being written to backend.log"
else
    print_error "Backend build failed!"
    exit 1
fi

cd ..

# Wait for backend to start
print_status "Waiting for backend to start..."
sleep 10

# Check if backend is running
if curl -f http://localhost:8080/api/auth/health > /dev/null 2>&1; then
    print_success "Backend is running and healthy!"
else
    print_warning "Backend might still be starting up..."
fi

# Frontend Setup (Simple HTML/JS version for quick demo)
print_status "Setting up Frontend..."

cd frontend

# Create a simple HTML frontend for demonstration
cat > index.html << 'EOF'
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Log Analyzer Dashboard</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.1.3/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.0.0/css/all.min.css" rel="stylesheet">
    <style>
        .log-level-error { color: #dc3545; }
        .log-level-warn { color: #ffc107; }
        .log-level-info { color: #17a2b8; }
        .log-level-debug { color: #6c757d; }
        .log-level-trace { color: #6f42c1; }
        .log-level-fatal { color: #e83e8c; }
        .sidebar { min-height: 100vh; background-color: #f8f9fa; }
        .main-content { padding: 20px; }
        .stat-card { transition: transform 0.2s; }
        .stat-card:hover { transform: translateY(-2px); }
        .log-entry { border-left: 4px solid #dee2e6; margin-bottom: 10px; padding: 10px; }
        .log-entry.error { border-left-color: #dc3545; }
        .log-entry.warn { border-left-color: #ffc107; }
        .log-entry.info { border-left-color: #17a2b8; }
        .log-entry.debug { border-left-color: #6c757d; }
        .log-entry.trace { border-left-color: #6f42c1; }
        .log-entry.fatal { border-left-color: #e83e8c; }
        .navbar-brand { font-weight: bold; }
        .user-info { background: rgba(255,255,255,0.1); border-radius: 20px; padding: 5px 15px; }
    </style>
</head>
<body>
    <!-- Navigation -->
    <nav class="navbar navbar-expand-lg navbar-dark bg-primary">
        <div class="container-fluid">
            <a class="navbar-brand" href="#"><i class="fas fa-chart-line me-2"></i>Log Analyzer</a>
            <div class="navbar-nav ms-auto">
                <div class="nav-item user-info text-white" id="userInfo">
                    <i class="fas fa-user me-2"></i><span id="username">Not logged in</span>
                </div>
            </div>
        </div>
    </nav>

    <div class="container-fluid">
        <div class="row">
            <!-- Sidebar -->
            <div class="col-md-2 sidebar p-3">
                <div class="nav flex-column">
                    <button class="btn btn-outline-primary mb-2" onclick="showLogin()">
                        <i class="fas fa-sign-in-alt me-2"></i>Login
                    </button>
                    <button class="btn btn-outline-secondary mb-2" onclick="loadDashboard()">
                        <i class="fas fa-tachometer-alt me-2"></i>Dashboard
                    </button>
                    <button class="btn btn-outline-info mb-2" onclick="loadLogs()">
                        <i class="fas fa-list me-2"></i>View Logs
                    </button>
                    <button class="btn btn-outline-success mb-2" onclick="generateLogs()">
                        <i class="fas fa-plus me-2"></i>Generate Logs
                    </button>
                    <button class="btn btn-outline-warning mb-2" onclick="toggleRealtime()">
                        <i class="fas fa-play me-2"></i>Realtime
                    </button>
                </div>
            </div>

            <!-- Main Content -->
            <div class="col-md-10 main-content">
                <div id="content">
                    <h2>Welcome to Log Analyzer</h2>
                    <p>Please login to access the dashboard.</p>
                </div>
            </div>
        </div>
    </div>

    <!-- Login Modal -->
    <div class="modal fade" id="loginModal" tabindex="-1">
        <div class="modal-dialog">
            <div class="modal-content">
                <div class="modal-header">
                    <h5 class="modal-title">Login</h5>
                    <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
                </div>
                <div class="modal-body">
                    <form id="loginForm">
                        <div class="mb-3">
                            <label class="form-label">Username</label>
                            <input type="text" class="form-control" id="username" value="admin">
                            <div class="form-text">Default users: admin/admin123, analyst/analyst123, viewer/viewer123</div>
                        </div>
                        <div class="mb-3">
                            <label class="form-label">Password</label>
                            <input type="password" class="form-control" id="password" value="admin123">
                        </div>
                    </form>
                </div>
                <div class="modal-footer">
                    <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Cancel</button>
                    <button type="button" class="btn btn-primary" onclick="login()">Login</button>
                </div>
            </div>
        </div>
    </div>

    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.1.3/dist/js/bootstrap.bundle.min.js"></script>
    <script src="app.js"></script>
</body>
</html>
EOF

# Create JavaScript application
cat > app.js << 'EOF'
const API_BASE = 'http://localhost:8080/api';
let authToken = localStorage.getItem('authToken');
let currentUser = null;
let realtimeActive = false;

// Initialize application
document.addEventListener('DOMContentLoaded', function() {
    if (authToken) {
        getCurrentUser();
    }
    loadDashboard();
});

// Authentication functions
function showLogin() {
    const modal = new bootstrap.Modal(document.getElementById('loginModal'));
    modal.show();
}

async function login() {
    const username = document.getElementById('username').value;
    const password = document.getElementById('password').value;
    
    try {
        const response = await fetch(`${API_BASE}/auth/signin`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({ username, password })
        });
        
        if (response.ok) {
            const data = await response.json();
            authToken = data.token;
            localStorage.setItem('authToken', authToken);
            localStorage.setItem('refreshToken', data.refreshToken);
            currentUser = data;
            
            document.getElementById('username').textContent = data.username;
            const modal = bootstrap.Modal.getInstance(document.getElementById('loginModal'));
            modal.hide();
            
            loadDashboard();
            showAlert('Login successful!', 'success');
        } else {
            const error = await response.json();
            showAlert(error.error || 'Login failed', 'danger');
        }
    } catch (error) {
        showAlert('Network error: ' + error.message, 'danger');
    }
}

async function getCurrentUser() {
    if (!authToken) return;
    
    try {
        const response = await fetch(`${API_BASE}/auth/me`, {
            headers: {
                'Authorization': `Bearer ${authToken}`
            }
        });
        
        if (response.ok) {
            currentUser = await response.json();
            document.getElementById('username').textContent = currentUser.username;
        }
    } catch (error) {
        console.error('Failed to get current user:', error);
    }
}

// Dashboard functions
async function loadDashboard() {
    if (!authToken) {
        document.getElementById('content').innerHTML = `
            <h2>Welcome to Log Analyzer</h2>
            <p>Please login to access the dashboard.</p>
        `;
        return;
    }
    
    try {
        const response = await fetch(`${API_BASE}/logs/statistics`, {
            headers: {
                'Authorization': `Bearer ${authToken}`
            }
        });
        
        if (response.ok) {
            const stats = await response.json();
            displayDashboard(stats);
        } else {
            showAlert('Failed to load dashboard', 'danger');
        }
    } catch (error) {
        showAlert('Network error: ' + error.message, 'danger');
    }
}

function displayDashboard(stats) {
    const content = document.getElementById('content');
    content.innerHTML = `
        <h2><i class="fas fa-tachometer-alt me-2"></i>Dashboard</h2>
        
        <div class="row mb-4">
            <div class="col-md-3">
                <div class="card stat-card bg-primary text-white">
                    <div class="card-body">
                        <h5><i class="fas fa-list me-2"></i>Total Logs</h5>
                        <h3>${stats.totalLogs}</h3>
                    </div>
                </div>
            </div>
            <div class="col-md-3">
                <div class="card stat-card bg-danger text-white">
                    <div class="card-body">
                        <h5><i class="fas fa-exclamation-triangle me-2"></i>Errors</h5>
                        <h3>${stats.errorCount || 0}</h3>
                    </div>
                </div>
            </div>
            <div class="col-md-3">
                <div class="card stat-card bg-warning text-white">
                    <div class="card-body">
                        <h5><i class="fas fa-exclamation me-2"></i>Warnings</h5>
                        <h3>${stats.warningCount || 0}</h3>
                    </div>
                </div>
            </div>
            <div class="col-md-3">
                <div class="card stat-card bg-info text-white">
                    <div class="card-body">
                        <h5><i class="fas fa-info me-2"></i>Info</h5>
                        <h3>${stats.infoCount || 0}</h3>
                    </div>
                </div>
            </div>
        </div>
        
        <div class="row">
            <div class="col-md-6">
                <div class="card">
                    <div class="card-header">
                        <h5><i class="fas fa-chart-pie me-2"></i>Log Level Distribution</h5>
                    </div>
                    <div class="card-body">
                        ${generateLogLevelChart(stats.logLevelCounts || {})}
                    </div>
                </div>
            </div>
            <div class="col-md-6">
                <div class="card">
                    <div class="card-header">
                        <h5><i class="fas fa-clock me-2"></i>Recent Activity</h5>
                    </div>
                    <div class="card-body">
                        <p>Recent logs (24h): <strong>${stats.recentLogsCount || 0}</strong></p>
                        <p>Last updated: <strong>${new Date(stats.lastUpdated).toLocaleString()}</strong></p>
                        <button class="btn btn-sm btn-outline-primary" onclick="loadRecentLogs()">
                            <i class="fas fa-eye me-1"></i>View Recent Logs
                        </button>
                    </div>
                </div>
            </div>
        </div>
    `;
}

function generateLogLevelChart(levelCounts) {
    let html = '<div class="row">';
    const levels = ['ERROR', 'WARN', 'INFO', 'DEBUG', 'TRACE', 'FATAL'];
    const colors = ['danger', 'warning', 'info', 'secondary', 'primary', 'dark'];
    
    levels.forEach((level, index) => {
        const count = levelCounts[level] || 0;
        html += `
            <div class="col-6 mb-2">
                <div class="d-flex justify-content-between align-items-center">
                    <span class="badge bg-${colors[index]}">${level}</span>
                    <strong>${count}</strong>
                </div>
            </div>
        `;
    });
    
    html += '</div>';
    return html;
}

// Log functions
async function loadLogs() {
    if (!authToken) {
        showAlert('Please login first', 'warning');
        return;
    }
    
    try {
        const response = await fetch(`${API_BASE}/logs?size=50`, {
            headers: {
                'Authorization': `Bearer ${authToken}`
            }
        });
        
        if (response.ok) {
            const data = await response.json();
            displayLogs(data.content || []);
        } else {
            showAlert('Failed to load logs', 'danger');
        }
    } catch (error) {
        showAlert('Network error: ' + error.message, 'danger');
    }
}

async function loadRecentLogs() {
    if (!authToken) {
        showAlert('Please login first', 'warning');
        return;
    }
    
    try {
        const response = await fetch(`${API_BASE}/logs/recent/60`, {
            headers: {
                'Authorization': `Bearer ${authToken}`
            }
        });
        
        if (response.ok) {
            const logs = await response.json();
            displayLogs(logs);
        } else {
            showAlert('Failed to load recent logs', 'danger');
        }
    } catch (error) {
        showAlert('Network error: ' + error.message, 'danger');
    }
}

function displayLogs(logs) {
    const content = document.getElementById('content');
    let html = `
        <div class="d-flex justify-content-between align-items-center mb-3">
            <h2><i class="fas fa-list me-2"></i>Logs (${logs.length})</h2>
            <button class="btn btn-primary" onclick="loadLogs()">
                <i class="fas fa-sync me-1"></i>Refresh
            </button>
        </div>
    `;
    
    if (logs.length === 0) {
        html += '<div class="alert alert-info">No logs found.</div>';
    } else {
        logs.forEach(log => {
            const levelClass = log.level.toLowerCase();
            const timestamp = new Date(log.timestamp).toLocaleString();
            html += `
                <div class="log-entry ${levelClass}">
                    <div class="d-flex justify-content-between align-items-start">
                        <div class="flex-grow-1">
                            <div class="d-flex align-items-center mb-1">
                                <span class="badge bg-${getLevelColor(log.level)} me-2">${log.level}</span>
                                <small class="text-muted">${timestamp}</small>
                                <small class="text-muted ms-2">${log.loggerName || 'Unknown'}</small>
                            </div>
                            <div class="log-message">${log.message}</div>
                            ${log.stackTrace ? `<details class="mt-2"><summary>Stack Trace</summary><pre class="small mt-2">${log.stackTrace}</pre></details>` : ''}
                        </div>
                    </div>
                </div>
            `;
        });
    }
    
    content.innerHTML = html;
}

function getLevelColor(level) {
    const colors = {
        'ERROR': 'danger',
        'WARN': 'warning', 
        'INFO': 'info',
        'DEBUG': 'secondary',
        'TRACE': 'primary',
        'FATAL': 'dark'
    };
    return colors[level] || 'secondary';
}

// Log generation functions
async function generateLogs() {
    if (!authToken) {
        showAlert('Please login first', 'warning');
        return;
    }
    
    const count = prompt('How many logs to generate?', '10');
    if (!count || isNaN(count)) return;
    
    try {
        const response = await fetch(`${API_BASE}/logs/generate?count=${count}`, {
            method: 'POST',
            headers: {
                'Authorization': `Bearer ${authToken}`
            }
        });
        
        if (response.ok) {
            const result = await response.json();
            showAlert(`Generated ${count} logs successfully!`, 'success');
            loadDashboard(); // Refresh dashboard
        } else {
            const error = await response.json();
            showAlert(error.error || 'Failed to generate logs', 'danger');
        }
    } catch (error) {
        showAlert('Network error: ' + error.message, 'danger');
    }
}

async function toggleRealtime() {
    if (!authToken) {
        showAlert('Please login first', 'warning');
        return;
    }
    
    try {
        const endpoint = realtimeActive ? 'stop' : 'start';
        const response = await fetch(`${API_BASE}/logs/generate/realtime/${endpoint}`, {
            method: 'POST',
            headers: {
                'Authorization': `Bearer ${authToken}`
            }
        });
        
        if (response.ok) {
            realtimeActive = !realtimeActive;
            const message = realtimeActive ? 'Real-time log generation started!' : 'Real-time log generation stopped!';
            showAlert(message, 'success');
        } else {
            const error = await response.json();
            showAlert(error.error || 'Failed to toggle real-time generation', 'danger');
        }
    } catch (error) {
        showAlert('Network error: ' + error.message, 'danger');
    }
}

// Utility functions
function showAlert(message, type) {
    const alertDiv = document.createElement('div');
    alertDiv.className = `alert alert-${type} alert-dismissible fade show position-fixed`;
    alertDiv.style.cssText = 'top: 20px; right: 20px; z-index: 9999; min-width: 300px;';
    alertDiv.innerHTML = `
        ${message}
        <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
    `;
    document.body.appendChild(alertDiv);
    
    setTimeout(() => {
        if (alertDiv.parentNode) {
            alertDiv.parentNode.removeChild(alertDiv);
        }
    }, 5000);
}

// Auto-refresh dashboard every 30 seconds if logged in
setInterval(() => {
    if (authToken && document.getElementById('content').innerHTML.includes('Dashboard')) {
        loadDashboard();
    }
}, 30000);
EOF

print_success "Frontend setup completed!"

# Start a simple HTTP server for the frontend
if command -v python3 &> /dev/null; then
    print_status "Starting frontend server on port 3000..."
    nohup python3 -m http.server 3000 > ../frontend.log 2>&1 &
    FRONTEND_PID=$!
    echo $FRONTEND_PID > ../frontend.pid
    print_success "Frontend started with PID: $FRONTEND_PID"
elif command -v node &> /dev/null; then
    print_status "Starting frontend server on port 3000..."
    npx http-server -p 3000 > ../frontend.log 2>&1 &
    FRONTEND_PID=$!
    echo $FRONTEND_PID > ../frontend.pid
    print_success "Frontend started with PID: $FRONTEND_PID"
else
    print_warning "No web server available. Please serve the frontend manually."
fi

cd ..

# Final status
print_success "🎉 Log Analyzer Application Setup Complete!"
echo ""
echo "📋 Application Information:"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "🔧 Backend (Spring Boot):"
echo "   • URL: http://localhost:8080"
echo "   • Health Check: http://localhost:8080/actuator/health"
echo "   • H2 Console: http://localhost:8080/h2-console"
echo "   • API Documentation: http://localhost:8080/api"
echo ""
echo "🎨 Frontend (Web Dashboard):"
echo "   • URL: http://localhost:3000"
echo "   • Login with: admin/admin123, analyst/analyst123, or viewer/viewer123"
echo ""
echo "📊 Default Users:"
echo "   • admin/admin123 (ADMIN + ANALYST + VIEWER roles)"
echo "   • analyst/analyst123 (ANALYST + VIEWER roles)"
echo "   • viewer/viewer123 (VIEWER role only)"
echo ""
echo "🔍 Monitoring:"
echo "   • Prometheus Metrics: http://localhost:8080/actuator/prometheus"
echo "   • Application Metrics: http://localhost:8080/actuator/metrics"
echo ""
echo "📝 Logs:"
echo "   • Backend logs: backend.log"
echo "   • Frontend logs: frontend.log"
echo "   • Application logs: backend/logs/application.log"
echo ""
echo "🛑 To stop the application:"
echo "   • Backend: kill \$(cat backend.pid)"
echo "   • Frontend: kill \$(cat frontend.pid)"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""
print_success "✅ Ready to use! Open http://localhost:3000 in your browser"