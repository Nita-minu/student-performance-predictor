# Academic Student Performance & At-Risk Predictor

A browser-based dashboard for reviewing student attendance, assessment results, and weekly study hours. It calculates an estimated risk score, sorts students by risk, highlights high-risk records, and provides a searchable performance report that can be downloaded as CSV.

## Features

- Load student records from a CSV file
- Calculate a risk score from 0% to 100%
- Classify students as low, medium, or high risk
- Sort students from highest to lowest risk
- Search by student ID or name
- View student counts by risk level
- Download a performance report as CSV
- Run in a web browser without installing Java or other software

## Risk score assumptions

The project abstract describes a multi-factor score but does not specify the formula. This version assumes:

- Attendance deficit: 40%
- Assessment deficit: 40%
- Study-hours deficit: 20%, based on a target of 10 hours per week
- Low risk: below 40%
- Medium risk: 40% to below 70%
- High risk: 70% or above


Attendance and assessment must be between 0 and 100. Study hours must be zero or greater.

## Run locally

1. Download or clone this repository.
2. Make sure the HTML file is named `index.html`.
3. Open `index.html` in a modern web browser.
4. Choose a CSV file, or select **Load sample data** to try the dashboard.

