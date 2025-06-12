#!/bin/bash

# Shell script to run different test strategies for multiple modules and save their JaCoCo coverage reports

PROJECT_DIR="/Users/wangxianbin/Downloads/毕设2/exception-test-amplifier"
REPORTS_BASE_DIR="${PROJECT_DIR}/coverage_reports"

# Define modules and their corresponding test classes
# Format: "module_name:TestClassName"
MODULES=(
    "stock:StockTradingResourceAmplifiedTest"
    "order:OrderManagementResourceAmplifiedTest"
    "wallet:WalletResourceAmplifiedTest"
)

# Define strategies to run for each module
# Format: "test_method_name report_subdir_name strategy_display_name"
STRATEGIES=(
    "testExhaustiveAmplification exhaustive_strategy_report Exhaustive"
    "testLLMAmplification llm_strategy_report LLM"
)

# Ensure we are in the project directory
cd "${PROJECT_DIR}" || {
    echo "Error: Could not navigate to project directory ${PROJECT_DIR}"
    exit 1
}

# Create base reports directory if it doesn't exist
mkdir -p "${REPORTS_BASE_DIR}"

echo "Starting coverage report generation for different modules and strategies..."

# Loop through each module
for module_info in "${MODULES[@]}"; do
    IFS=':' read -r module_name test_class_name <<< "$module_info"

    echo ""
    echo "========================================================================"
    echo "Processing Module: ${module_name}"
    echo "========================================================================"

    # Loop through each strategy for the current module
    for strategy_info in "${STRATEGIES[@]}"; do
        read -r test_method report_subdir strategy_name <<<"${strategy_info}"

        echo ""
        echo "------------------------------------------------------------------------"
        echo "Running Strategy: ${strategy_name} (Test: ${test_class_name}#${test_method})"
        echo "------------------------------------------------------------------------"

        # Run Maven command for the specific test method
        mvn clean verify -Dtest="${test_class_name}#${test_method}"

        # Check if Maven command was successful
        if [ $? -eq 0 ]; then
            echo "Maven build successful for ${module_name} - ${strategy_name}."

            # Define module-specific report path
            MODULE_REPORT_PATH="${REPORTS_BASE_DIR}/${module_name}/${report_subdir}"
            mkdir -p "${MODULE_REPORT_PATH}"

            if [ -d "target/site/jacoco" ]; then
                echo "Copying JaCoCo report to ${MODULE_REPORT_PATH}"
                cp -R target/site/jacoco/* "${MODULE_REPORT_PATH}/"
                echo "Report for ${module_name} - ${strategy_name} saved to ${MODULE_REPORT_PATH}"
            else
                echo "Error: JaCoCo report directory (target/site/jacoco) not found after build for ${module_name} - ${strategy_name}."
            fi
        else
            echo "Error: Maven build failed for ${module_name} - ${strategy_name}. Stopping script."
            exit 1
        fi
    done
done

echo ""
echo "========================================================================"
echo "All coverage reports generated successfully."
echo "Reports are located in: ${REPORTS_BASE_DIR}"
echo "========================================================================"
