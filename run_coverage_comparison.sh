#!/bin/bash

# Shell script to run different test strategies and save their JaCoCo coverage reports

PROJECT_DIR="/Users/wangxianbin/Downloads/毕设2/exception-test-amplifier"
REPORTS_BASE_DIR="${PROJECT_DIR}/coverage_reports"
TEST_CLASS_NAME="StockTradingResourceAmplifiedTest"

# Define strategies: array of "test_method_name report_subdir_name strategy_display_name"
STRATEGIES=(
    "testExhaustiveAmplification exhaustive_strategy_report Exhaustive_Strategy"
    "testHighRiskOnlyAmplification high_risk_strategy_report High_Risk_Strategy"
    "testLLMAmplification llm_strategy_report LLM_Strategy"
)

# Ensure we are in the project directory
cd "${PROJECT_DIR}" || {
    echo "Error: Could not navigate to project directory ${PROJECT_DIR}"
    exit 1
}

# Create base reports directory if it doesn't exist
mkdir -p "${REPORTS_BASE_DIR}"

echo "Starting coverage report generation for different strategies..."

for strategy_info in "${STRATEGIES[@]}"; do
    # Split the string into an array
    read -r test_method report_subdir strategy_name <<<"${strategy_info}"

    echo ""
    echo "------------------------------------------------------------------------"
    echo "Processing Strategy: ${strategy_name} (Method: ${test_method})"
    echo "------------------------------------------------------------------------"

    # Run Maven command for the specific test method
    mvn clean verify -Dtest="${TEST_CLASS_NAME}#${test_method}"

    # Check if Maven command was successful
    if [ $? -eq 0 ]; then
        echo "Maven build successful for ${strategy_name}."
        
        REPORT_PATH="${REPORTS_BASE_DIR}/${report_subdir}"
        mkdir -p "${REPORT_PATH}"
        
        if [ -d "target/site/jacoco" ]; then
            echo "Copying JaCoCo report to ${REPORT_PATH}"
            cp -R target/site/jacoco/* "${REPORT_PATH}/"
            echo "Report for ${strategy_name} saved to ${REPORT_PATH}"
        else
            echo "Error: JaCoCo report directory (target/site/jacoco) not found after build for ${strategy_name}."
        fi
    else
        echo "Error: Maven build failed for ${strategy_name}. Stopping script."
        exit 1
    fi
done

echo ""
echo "------------------------------------------------------------------------"
echo "All coverage reports generated successfully."
echo "Reports are located in: ${REPORTS_BASE_DIR}"
echo "------------------------------------------------------------------------"
