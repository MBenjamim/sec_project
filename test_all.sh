#!/bin/bash

# Colors for output
YELLOW='\033[1;33m'
GREEN='\033[0;32m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# Counters for passed and failed tests
passed_count=0
failed_count=0

# Run all test scripts in the tests_sh folder
for test_script in tests_sh/test*/test*.sh; do
    echo -e "${YELLOW}Running $test_script...${NC}"
    output=$(bash "$test_script")
    echo "$output"
    if echo "$output" | grep -q "\[PASSED\]"; then
        ((passed_count++))
    else
        ((failed_count++))
    fi
done

echo -e "${YELLOW}All tests completed.${NC}"
echo -e "${GREEN}Passed: $passed_count${NC}"
echo -e "${RED}Failed: $failed_count${NC}"