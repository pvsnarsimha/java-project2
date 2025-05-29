document.addEventListener('DOMContentLoaded', () => {
    console.log('chartRenderer.js loaded at', new Date().toISOString());

    // Verify Chart.js is loaded
    if (typeof Chart === 'undefined') {
        console.error('Chart.js not loaded. Charts will not be rendered.');
        return;
    }
    console.log('Chart.js is loaded successfully');

    // Function to parse sugar values from table cells (e.g., "124 mg/dL" -> 124)
    function parseSugarValue(text) {
        if (!text || typeof text !== 'string') {
            console.warn('Invalid text for parsing sugar value:', text);
            return NaN;
        }
        const numericText = text.replace(/[^0-9.]/g, '');
        const value = parseFloat(numericText);
        if (isNaN(value)) {
            console.warn(`Failed to parse sugar value from text: ${text}`);
        }
        return value;
    }

    // Function to render a chart for a given period and patient
    function renderChart(recordsBodyId, chartCanvasId, periodLabel) {
        console.log(`Attempting to render chart for ${chartCanvasId}, recordsBody: ${recordsBodyId}, period: ${periodLabel}`);

        const chartCanvas = document.getElementById(chartCanvasId);
        if (!chartCanvas) {
            console.error(`Chart canvas not found: ${chartCanvasId}`);
            return;
        }
        console.log(`Found chart canvas: ${chartCanvasId}`);

        // Find the table body (optional, we'll render "No Data" if missing)
        const recordsBody = document.getElementById(recordsBodyId);
        let dates = [];
        let fastingValues = [];
        let postPrandialValues = [];
        let hasValidData = false;

        if (!recordsBody) {
            console.warn(`Table body not found: ${recordsBodyId}. Rendering chart with "No Data".`);
        } else {
            console.log(`Found table body: ${recordsBodyId}`);
            const rows = recordsBody.getElementsByTagName('tr');
            console.log(`Found ${rows.length} rows in ${recordsBodyId}`);
            for (let row of rows) {
                const cells = row.getElementsByTagName('td');
                if (cells.length >= 5) {
                    const dateText = cells[0].innerText || 'N/A';
                    const fastingText = cells[1].innerText;
                    const postPrandialText = cells[3].innerText;

                    // Skip rows with "No records found"
                    if (dateText.toLowerCase().includes('no records found')) {
                        console.log(`Skipping row with 'No records found' for ${recordsBodyId}`);
                        continue;
                    }

                    const fastingValue = parseSugarValue(fastingText);
                    const postPrandialValue = parseSugarValue(postPrandialText);

                    console.log(`Row data - Date: ${dateText}, Fasting: ${fastingText} -> ${fastingValue}, Postprandial: ${postPrandialText} -> ${postPrandialValue}`);
                    if (!isNaN(fastingValue) || !isNaN(postPrandialValue)) {
                        dates.push(dateText);
                        fastingValues.push(isNaN(fastingValue) ? null : fastingValue); // Use null for missing data in line graphs
                        postPrandialValues.push(isNaN(postPrandialValue) ? null : postPrandialValue);
                        hasValidData = true;
                    }
                }
            }
        }

        // Get the canvas context
        const ctx = chartCanvas.getContext('2d');
        if (!ctx) {
            console.error(`Failed to get 2D context for ${chartCanvasId}`);
            return;
        }

        // Destroy any existing chart on the canvas
        const existingChart = Chart.getChart(chartCanvas);
        if (existingChart) {
            console.log(`Destroying existing chart for ${chartCanvasId}`);
            existingChart.destroy();
        }

        // Render the line graph
        console.log(`Rendering line graph with data - Dates: ${dates}, Fasting: ${fastingValues}, Postprandial: ${postPrandialValues}`);
        new Chart(ctx, {
            type: 'line',
            data: {
                labels: hasValidData ? dates : ['No Data'],
                datasets: [
                    {
                        label: 'Fasting Sugar (mg/dL)',
                        data: hasValidData ? fastingValues : [0],
                        borderColor: 'rgba(255, 107, 107, 1)',
                        backgroundColor: 'rgba(255, 107, 107, 0.2)',
                        fill: true,
                        tension: 0.3, // Smooth the line
                        pointRadius: 4,
                        pointBackgroundColor: 'rgba(255, 107, 107, 1)'
                    },
                    {
                        label: 'Postprandial Sugar (mg/dL)',
                        data: hasValidData ? postPrandialValues : [0],
                        borderColor: 'rgba(46, 204, 113, 1)',
                        backgroundColor: 'rgba(46, 204, 113, 0.2)',
                        fill: true,
                        tension: 0.3,
                        pointRadius: 4,
                        pointBackgroundColor: 'rgba(46, 204, 113, 1)'
                    }
                ]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                scales: {
                    y: {
                        beginAtZero: true,
                        max: 350,
                        ticks: { stepSize: 50, color: '#000' },
                        title: { display: true, text: 'Sugar Level (mg/dL)', color: '#000' },
                        grid: { color: 'rgba(0, 0, 0, 0.1)' }
                    },
                    x: {
                        title: { display: true, text: 'Date', color: '#000' },
                        ticks: { color: '#000' },
                        grid: { color: 'rgba(0, 0, 0, 0.1)' }
                    }
                },
                plugins: {
                    legend: { labels: { color: '#000' } },
                    title: {
                        display: !hasValidData,
                        text: `No ${periodLabel} Available`,
                        color: '#000',
                        font: { size: 16 }
                    }
                }
            }
        });
        console.log(`Line graph successfully rendered for ${chartCanvasId} with ${hasValidData ? dates.length : 0} data points`);
    }

    // Function to initialize charts for all patients and periods
    function initializeCharts() {
        const patientSections = document.querySelectorAll('.patient-section');
        console.log(`Found ${patientSections.length} patient sections`);

        if (patientSections.length === 0) {
            console.warn('No patient sections found. Charts will not be rendered.');
            return;
        }

        patientSections.forEach(section => {
            const patientName = section.getAttribute('data-patient-name');
            if (!patientName) {
                console.error('Missing data-patient-name attribute on patient section');
                return;
            }

            const sanitizedPatientName = patientName.replace(/[^a-zA-Z0-9-_]/g, '_');
            console.log(`Processing patient: ${patientName}, sanitized: ${sanitizedPatientName}`);

            const periods = [
                { id: 'today', label: "Today's Records" },
                { id: 'yesterday', label: "Yesterday's Records" },
                { id: 'week', label: "Last Week's Records" },
                { id: 'month', label: "Last Month's Records" },
                { id: 'year', label: "Last Year's Records" }
            ];

            periods.forEach(period => {
                const recordsBodyId = `recordsBody-${period.id}-${sanitizedPatientName}`;
                const chartCanvasId = `sugarChart-${period.id}-${sanitizedPatientName}`;

                // Log the expected canvas ID
                console.log(`Setting up MutationObserver for canvas: ${chartCanvasId}`);

                // Wait for the canvas element to appear in the DOM
                const observer = new MutationObserver((mutations, obs) => {
                    const chartCanvas = document.getElementById(chartCanvasId);
                    if (chartCanvas) {
                        console.log(`Canvas ${chartCanvasId} detected by MutationObserver`);
                        renderChart(recordsBodyId, chartCanvasId, period.label);
                        obs.disconnect(); // Stop observing once the canvas is found
                    }
                });

                observer.observe(document.body, {
                    childList: true,
                    subtree: true
                });

                // Fallback: Check immediately in case the element is already present
                const chartCanvas = document.getElementById(chartCanvasId);
                if (chartCanvas) {
                    console.log(`Canvas ${chartCanvasId} already present, rendering immediately`);
                    renderChart(recordsBodyId, chartCanvasId, period.label);
                    observer.disconnect();
                } else {
                    console.log(`Canvas ${chartCanvasId} not found immediately, relying on MutationObserver`);
                }
            });
        });
    }

    // Initialize charts when the DOM is loaded
    console.log('Initializing charts');
    initializeCharts();

    // Observe changes to patientDashboards for dynamic updates
    const patientDashboards = document.getElementById('patientDashboards');
    if (patientDashboards) {
        console.log('Setting up MutationObserver for patientDashboards');
        new MutationObserver(() => {
            console.log('Detected changes in patientDashboards, reinitializing charts');
            initializeCharts();
        }).observe(patientDashboards, { childList: true, subtree: true });
    } else {
        console.error('patientDashboards element not found');
    }
});