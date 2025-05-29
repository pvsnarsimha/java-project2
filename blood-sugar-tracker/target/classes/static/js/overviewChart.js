document.addEventListener('DOMContentLoaded', () => {
    console.log('overviewChart.js loaded at', new Date().toISOString());

    // Verify Chart.js is loaded
    if (typeof Chart === 'undefined') {
        console.error('Chart.js not loaded. Bar graph will not be rendered.');
        return;
    }
    console.log('Chart.js is loaded successfully for overviewChart');

    // Function to parse sugar values (e.g., "124 mg/dL" -> 124)
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

    // Function to render the bar graph
    function renderOverviewChart() {
        const recordsBody = document.getElementById('recordsBody');
        const chartCanvas = document.getElementById('overviewChart');

        if (!recordsBody) {
            console.error('Records body not found: recordsBody');
            return;
        }
        if (!chartCanvas) {
            console.error('Chart canvas not found: overviewChart');
            return;
        }
        console.log('Found recordsBody and overviewChart canvas');

        const dates = [];
        const fastingValues = [];
        const postPrandialValues = [];
        let hasValidData = false;

        const rows = recordsBody.getElementsByTagName('tr');
        console.log(`Found ${rows.length} rows in recordsBody`);
        for (let row of rows) {
            const cells = row.getElementsByTagName('td');
            if (cells.length >= 6) {
                const dateText = cells[1].innerText || 'N/A'; // Date is in the second column
                const fastingText = cells[2].innerText; // Fasting value
                const postPrandialText = cells[4].innerText; // Postprandial value

                const fastingValue = parseSugarValue(fastingText);
                const postPrandialValue = parseSugarValue(postPrandialText);

                console.log(`Row data - Date: ${dateText}, Fasting: ${fastingText} -> ${fastingValue}, Postprandial: ${postPrandialText} -> ${postPrandialValue}`);
                if (!isNaN(fastingValue) || !isNaN(postPrandialValue)) {
                    dates.push(dateText);
                    fastingValues.push(isNaN(fastingValue) ? 0 : fastingValue);
                    postPrandialValues.push(isNaN(postPrandialValue) ? 0 : postPrandialValue);
                    hasValidData = true;
                }
            }
        }

        const ctx = chartCanvas.getContext('2d');
        if (!ctx) {
            console.error('Failed to get 2D context for overviewChart');
            return;
        }

        // Destroy any existing chart
        const existingChart = Chart.getChart(chartCanvas);
        if (existingChart) {
            console.log('Destroying existing chart for overviewChart');
            existingChart.destroy();
        }

        // Render the bar graph
        console.log(`Rendering bar graph with data - Dates: ${dates}, Fasting: ${fastingValues}, Postprandial: ${postPrandialValues}`);
        new Chart(ctx, {
            type: 'bar',
            data: {
                labels: hasValidData ? dates : ['No Data'],
                datasets: [
                    {
                        label: 'Fasting Sugar (mg/dL)',
                        data: hasValidData ? fastingValues : [0],
                        backgroundColor: 'rgba(255, 107, 107, 0.6)',
                        borderColor: 'rgba(255, 107, 107, 1)',
                        borderWidth: 1
                    },
                    {
                        label: 'Postprandial Sugar (mg/dL)',
                        data: hasValidData ? postPrandialValues : [0],
                        backgroundColor: 'rgba(46, 204, 113, 0.6)',
                        borderColor: 'rgba(46, 204, 113, 1)',
                        borderWidth: 1
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
                        text: 'No Records Available',
                        color: '#000',
                        font: { size: 16 }
                    }
                }
            }
        });
        console.log(`Bar graph rendered for overviewChart with ${hasValidData ? dates.length : 0} data points`);
    }

    // Initialize the chart
    console.log('Initializing overview chart');
    renderOverviewChart();
});