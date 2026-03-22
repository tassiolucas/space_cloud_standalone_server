document.addEventListener('DOMContentLoaded', function() {
    // Cache DOM elements
    const configForm = document.getElementById('config-form');
    const advancedForm = document.getElementById('advanced-config-form');
    const pingTimeoutInput = document.getElementById('ping-timeout');
    const pingIntervalInput = document.getElementById('ping-interval');
    const maxLogEntriesInput = document.getElementById('max-log-entries');
    const ipInput = document.getElementById('ip-address');
    const macInput = document.getElementById('mac-address');
    const hourInput = document.getElementById('hour');
    const minuteInput = document.getElementById('minute');
    const enableToggle = document.getElementById('enable-schedule');
    const testWolButton = document.getElementById('test-wol');
    const statusText = document.getElementById('status-text');
    const serverIcon = document.getElementById('server-icon');
    const nextWakeTime = document.getElementById('next-wake-time');
    const notification = document.getElementById('notification');
    const notificationMessage = document.getElementById('notification-message');
    const closeNotification = document.getElementById('close-notification');
    const toggleStatusText = document.getElementById('toggle-status-text');
    const uptimeElement = document.getElementById('uptime');
    const lastWakeElement = document.getElementById('last-wake');
    const logEntriesElement = document.getElementById('log-entries');
    const totalActivations = document.getElementById('total-activations');
    const successfulActivations = document.getElementById('successful-activations');
    const successRate = document.getElementById('success-rate');
    const refreshLogsButton = document.getElementById('refresh-logs');
    const clearLogsButton = document.getElementById('clear-logs');
    const navLinks = document.querySelectorAll('.nav-links a');
    const contentSections = document.querySelectorAll('.content-section');

    let serverStartMillis = null;
    let logPollInterval = null;

    // Inicialização
    loadConfiguration();
    loadLogs();
    updateStatus();
    fetchServerUptime();

    setInterval(updateStatus, 30000);
    setInterval(updateUptime, 1000);
    setInterval(loadLogs, 60000);

    // Event listeners
    configForm.addEventListener('submit', saveConfiguration);
    advancedForm.addEventListener('submit', saveAdvancedConfiguration);
    testWolButton.addEventListener('click', testWakeOnLan);
    closeNotification.addEventListener('click', hideNotification);
    enableToggle.addEventListener('change', updateToggleStatus);
    refreshLogsButton.addEventListener('click', loadLogs);
    clearLogsButton.addEventListener('click', clearLogs);

    navLinks.forEach(link => {
        link.addEventListener('click', function(e) {
            e.preventDefault();
            const sectionId = this.getAttribute('data-section');
            navLinks.forEach(l => l.classList.remove('active'));
            this.classList.add('active');
            contentSections.forEach(s => s.classList.remove('active'));
            document.getElementById(`${sectionId}-section`).classList.add('active');
        });
    });

    // Validação de inputs
    ipInput.addEventListener('input', function() {
        validateInput(this, /^(\d{1,3}\.){3}\d{1,3}$/);
    });
    macInput.addEventListener('input', function() {
        validateInput(this, /^([0-9A-Fa-f]{2}[:-]){5}([0-9A-Fa-f]{2})$/);
    });

    // ---- Funções ----

    function loadConfiguration() {
        fetch('/api/config')
            .then(r => r.json())
            .then(config => {
                ipInput.value = config.ip || '192.168.1.100';
                macInput.value = config.mac || '00:11:22:33:44:55';
                hourInput.value = config.hour || '07';
                minuteInput.value = config.minute || '00';
                enableToggle.checked = config.enabled || false;
                pingTimeoutInput.value = config.ping_timeout_seconds || 120;
                pingIntervalInput.value = config.ping_interval_seconds || 5;
                maxLogEntriesInput.value = config.max_log_entries || 50;
                updateToggleStatus();
                updateNextWakeTime();
            })
            .catch(err => showNotification('Erro ao carregar configurações: ' + err.message, 'error'));
    }

    function saveAdvancedConfiguration(event) {
        event.preventDefault();
        const config = {
            ping_timeout_seconds: parseInt(pingTimeoutInput.value),
            ping_interval_seconds: parseInt(pingIntervalInput.value),
            max_log_entries: parseInt(maxLogEntriesInput.value)
        };

        fetch('/api/config', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(config)
        })
        .then(r => r.json())
        .then(data => {
            if (data.success) showNotification('Configurações avançadas salvas!', 'success');
            else showNotification('Erro ao salvar', 'error');
        })
        .catch(err => showNotification('Erro: ' + err.message, 'error'));
    }

    function loadLogs() {
        fetch('/api/logs')
            .then(r => r.json())
            .then(logData => {
                updateLogTable(logData);
                updateStatistics(logData);
                updateLastWakeTime(logData);
            })
            .catch(err => console.error('Erro ao carregar logs:', err));
    }

    function updateLogTable(logData) {
        if (!logData.logs || logData.logs.length === 0) {
            logEntriesElement.innerHTML = '<tr class="no-logs"><td colspan="6">Nenhum log disponível</td></tr>';
            return;
        }

        logEntriesElement.innerHTML = '';
        logData.logs.forEach(log => {
            const row = document.createElement('tr');
            let statusClass, statusIcon;

            if (log.successful) {
                statusClass = 'status-success';
                statusIcon = '<i class="fas fa-check-circle"></i>';
            } else if (log.retry_attempt > 0 && log.retry_attempt < 3) {
                statusClass = 'status-retry';
                statusIcon = '<i class="fas fa-sync"></i>';
            } else {
                statusClass = 'status-failed';
                statusIcon = '<i class="fas fa-times-circle"></i>';
            }

            row.innerHTML = `
                <td>${log.timestamp}</td>
                <td>${log.ip}</td>
                <td>${log.mac}</td>
                <td>${log.retry_attempt > 0 ? log.retry_attempt + '/3' : '1/3'}</td>
                <td class="${statusClass}">${statusIcon} ${log.successful ? 'Sucesso' : 'Falha'}</td>
                <td>${log.message}</td>
            `;
            logEntriesElement.appendChild(row);
        });
    }

    function updateStatistics(logData) {
        totalActivations.textContent = logData.total_attempts || 0;
        successfulActivations.textContent = logData.successful_activations || 0;
        const rate = logData.total_attempts > 0
            ? Math.round((logData.successful_activations / logData.total_attempts) * 100)
            : 0;
        successRate.textContent = `${rate}%`;
    }

    function updateLastWakeTime(logData) {
        lastWakeElement.textContent = logData.last_successful_time || 'Nenhum';
    }

    function clearLogs() {
        if (confirm('Tem certeza que deseja limpar todos os logs?')) {
            fetch('/api/clear-logs', { method: 'POST' })
                .then(r => r.json())
                .then(data => {
                    if (data.success) {
                        showNotification('Logs limpos com sucesso', 'success');
                        loadLogs();
                    } else {
                        showNotification('Erro ao limpar logs', 'error');
                    }
                })
                .catch(err => showNotification('Erro ao limpar logs: ' + err.message, 'error'));
        }
    }

    function saveConfiguration(event) {
        event.preventDefault();
        if (!configForm.checkValidity()) {
            showNotification('Por favor, corrija os erros no formulário', 'error');
            return;
        }

        const config = {
            ip: ipInput.value,
            mac: macInput.value,
            hour: hourInput.value.padStart(2, '0'),
            minute: minuteInput.value.padStart(2, '0'),
            enabled: enableToggle.checked
        };

        fetch('/api/config', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(config)
        })
        .then(r => r.json())
        .then(data => {
            if (data.success) {
                showNotification('Configurações salvas com sucesso!', 'success');
                updateNextWakeTime();
                updateStatus();
                updateToggleStatus();
            } else {
                showNotification('Erro ao salvar configurações', 'error');
            }
        })
        .catch(err => showNotification('Erro ao salvar: ' + err.message, 'error'));
    }

    function testWakeOnLan() {
        testWolButton.disabled = true;
        testWolButton.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Enviando...';

        fetch('/api/test-wol', { method: 'POST' })
            .then(r => r.json())
            .then(data => {
                testWolButton.disabled = false;
                testWolButton.innerHTML = '<i class="fas fa-bolt"></i> Acionar Agora';

                if (data.success) {
                    showNotification(data.message, 'info');
                    // Polling de logs por 2 minutos após o trigger para acompanhar resultado
                    startLogPolling();
                } else {
                    showNotification('Erro ao enviar pacote: ' + data.message, 'error');
                }
            })
            .catch(err => {
                testWolButton.disabled = false;
                testWolButton.innerHTML = '<i class="fas fa-bolt"></i> Acionar Agora';
                showNotification('Erro ao acionar WOL: ' + err.message, 'error');
            });
    }

    function startLogPolling() {
        // Para qualquer polling anterior
        if (logPollInterval) clearInterval(logPollInterval);

        let pollCount = 0;
        const maxPolls = 24; // 24 * 5s = 120s

        loadLogs();
        logPollInterval = setInterval(() => {
            loadLogs();
            pollCount++;
            if (pollCount >= maxPolls) {
                clearInterval(logPollInterval);
                logPollInterval = null;
            }
        }, 5000);
    }

    function updateStatus() {
        fetch('/api/config')
            .then(r => r.json())
            .then(config => {
                if (config.enabled) {
                    serverIcon.className = 'fas fa-power-off active';
                    statusText.textContent = 'Agendamento Ativo';
                    statusText.className = 'status-active';
                } else {
                    serverIcon.className = 'fas fa-power-off';
                    statusText.textContent = 'Agendamento Inativo';
                    statusText.className = '';
                }
                updateNextWakeTime();
            })
            .catch(err => console.error('Erro ao atualizar status:', err));
    }

    function updateToggleStatus() {
        if (enableToggle.checked) {
            toggleStatusText.textContent = 'Ativo';
            toggleStatusText.className = 'toggle-status active';
        } else {
            toggleStatusText.textContent = 'Inativo';
            toggleStatusText.className = 'toggle-status';
        }
    }

    function updateNextWakeTime() {
        if (enableToggle.checked) {
            nextWakeTime.textContent = `${hourInput.value.padStart(2, '0')}:${minuteInput.value.padStart(2, '0')}`;
            nextWakeTime.className = 'time-display active';
        } else {
            nextWakeTime.textContent = '--:--';
            nextWakeTime.className = 'time-display';
        }
    }

    function fetchServerUptime() {
        fetch('/api/uptime')
            .then(r => r.json())
            .then(data => {
                serverStartMillis = Date.now() - data.uptime_millis;
                updateUptime();
            })
            .catch(() => {
                // fallback: usa o tempo de carregamento da página
                serverStartMillis = Date.now();
                updateUptime();
            });
    }

    function updateUptime() {
        if (serverStartMillis === null) return;
        const diff = Date.now() - serverStartMillis;
        const h = Math.floor(diff / 3600000);
        const m = Math.floor((diff % 3600000) / 60000);
        const s = Math.floor((diff % 60000) / 1000);
        uptimeElement.textContent = `${String(h).padStart(2,'0')}:${String(m).padStart(2,'0')}:${String(s).padStart(2,'0')}`;
    }

    function showNotification(message, type = 'info') {
        notificationMessage.textContent = message;
        notification.className = `notification ${type}`;
        notification.style.display = 'flex';
        setTimeout(hideNotification, 5000);
    }

    function hideNotification() {
        notification.style.display = 'none';
    }

    function validateInput(input, pattern) {
        input.setCustomValidity(pattern.test(input.value) ? '' : 'Formato inválido');
    }

    hourInput.addEventListener('blur', function() {
        let v = parseInt(this.value);
        if (isNaN(v) || v < 0) v = 0;
        if (v > 23) v = 23;
        this.value = String(v).padStart(2, '0');
        updateNextWakeTime();
    });

    minuteInput.addEventListener('blur', function() {
        let v = parseInt(this.value);
        if (isNaN(v) || v < 0) v = 0;
        if (v > 59) v = 59;
        this.value = String(v).padStart(2, '0');
        updateNextWakeTime();
    });
});
