#!/bin/bash

set -e

echo "🔧 Setting up code quality tools for Ktor project..."

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

# Check if required tools are installed
check_requirements() {
    print_status "Checking requirements..."

    if ! command -v java &> /dev/null; then
        print_error "Java is not installed. Please install Java 17 or later."
        exit 1
    fi

    if ! command -v git &> /dev/null; then
        print_error "Git is not installed. Please install Git."
        exit 1
    fi

    # Check Java version
    JAVA_VERSION=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}' | awk -F '.' '{print $1}')
    if [ "$JAVA_VERSION" -lt 17 ]; then
        print_warning "Java version is $JAVA_VERSION. Java 17 or later is recommended."
    fi

    print_success "Requirements check passed!"
}

# Create directory structure and required files
create_directories() {
    print_status "Creating directory structure..."

    mkdir -p config/detekt
    mkdir -p .github/workflows
    mkdir -p scripts

    print_success "Directory structure created!"

    # Check if detekt config exists
    if [ ! -f "config/detekt/detekt.yml" ]; then
        print_warning "Detekt configuration file not found at config/detekt/detekt.yml"
        print_status "Please create the detekt.yml file in the config/detekt/ directory"
        print_status "You can use the provided detekt.yml configuration from the artifacts"
    fi
}

# Install pre-commit
install_precommit() {
    print_status "Installing pre-commit..."

    if command -v python3 &> /dev/null; then
        if ! command -v pre-commit &> /dev/null; then
            if command -v pip3 &> /dev/null; then
                pip3 install pre-commit
            else
                print_warning "pip3 not found. Please install pre-commit manually: https://pre-commit.com/"
                return
            fi
        fi

        if [ -f ".pre-commit-config.yaml" ]; then
            print_status "Installing pre-commit hooks..."
            pre-commit install
            pre-commit install --hook-type commit-msg
            print_success "Pre-commit hooks installed!"
        else
            print_warning ".pre-commit-config.yaml not found. Please ensure it exists."
        fi
    else
        print_warning "Python3 not found. Please install pre-commit manually: https://pre-commit.com/"
    fi
}

# Initialize secrets baseline
init_secrets_baseline() {
    print_status "Initializing secrets baseline..."

    if command -v detect-secrets &> /dev/null; then
        detect-secrets scan --baseline .secrets.baseline
        print_success "Secrets baseline created!"
    else
        print_warning "detect-secrets not installed. Run: pip install detect-secrets"
        # Create empty baseline
        echo '{}' > .secrets.baseline
    fi
}

# Run initial quality checks
run_initial_checks() {
    print_status "Running initial quality checks..."

    if [ -f "gradlew" ]; then
        chmod +x gradlew

        print_status "Running Ktlint format..."
        if ./gradlew ktlintFormat; then
            print_success "Ktlint formatting completed!"
        else
            print_warning "Ktlint formatting had issues. Please review."
        fi

        print_status "Running quality check..."
        if ./gradlew qualityCheck; then
            print_success "All quality checks passed!"
        else
            print_warning "Some quality checks failed. Please review and fix issues."
        fi

        print_status "Generating coverage report..."
        ./gradlew test koverHtmlReport || print_warning "Coverage report generation had issues."

    else
        print_error "gradlew not found. Please ensure you're in the project root directory."
    fi
}

# Create useful aliases and scripts
create_convenience_scripts() {
    print_status "Creating convenience scripts..."

    # Create quality check script
    cat > scripts/quality-check.sh << 'EOF'
#!/bin/bash
echo "🔍 Running quality checks..."
./gradlew qualityCheck
echo "✅ Quality checks completed!"
EOF
    chmod +x scripts/quality-check.sh

    # Create fix script
    cat > scripts/quality-fix.sh << 'EOF'
#!/bin/bash
echo "🔧 Fixing auto-fixable quality issues..."
./gradlew qualityFix
echo "✅ Auto-fixes completed!"
EOF
    chmod +x scripts/quality-fix.sh

    # Create coverage script
    cat > scripts/coverage-report.sh << 'EOF'
#!/bin/bash
echo "📊 Generating coverage report..."
./gradlew test koverHtmlReport
echo "✅ Coverage report generated!"
echo "📁 Open: build/reports/kover/html/index.html"
EOF
    chmod +x scripts/coverage-report.sh

    print_success "Convenience scripts created in scripts/ directory!"
}

# Display setup completion info
display_completion_info() {
    echo
    print_success "🎉 Code quality tools setup completed successfully!"
    echo
    echo -e "${BLUE}Available commands:${NC}"
    echo "  ./gradlew qualityCheck      - Run all quality checks"
    echo "  ./gradlew qualityFix        - Fix auto-fixable issues"
    echo "  ./gradlew ktlintCheck       - Run Ktlint linting"
    echo "  ./gradlew ktlintFormat      - Format code with Ktlint"
    echo "  ./gradlew detekt            - Run Detekt analysis"
    echo "  ./gradlew test koverHtmlReport - Generate coverage report"
    echo
    echo -e "${BLUE}Convenience scripts:${NC}"
    echo "  ./scripts/quality-check.sh  - Quick quality check"
    echo "  ./scripts/quality-fix.sh    - Quick auto-fix"
    echo "  ./scripts/coverage-report.sh - Quick coverage report"
    echo
    echo -e "${BLUE}Reports locations:${NC}"
    echo "  📁 Ktlint:     build/reports/ktlint/"
    echo "  📁 Detekt:     build/reports/detekt/"
    echo "  📁 Coverage:   build/reports/kover/html/"
    echo "  📁 Tests:      build/reports/tests/"
    echo
    echo -e "${YELLOW}Next steps:${NC}"
    echo "  1. Update sonar-project.properties with your SonarCloud details"
    echo "  2. Add SONAR_TOKEN to your GitHub repository secrets"
    echo "  3. Commit and push your changes"
    echo "  4. Review the generated reports"
    echo
    echo -e "${BLUE}File structure created:${NC}"
    echo "  📁 config/detekt/           - Detekt configuration"
    echo "  📁 .github/workflows/       - CI/CD workflows"
    echo "  📁 scripts/                 - Utility scripts"
    echo "  📄 .pre-commit-config.yaml  - Pre-commit hooks"
    echo "  📄 sonar-project.properties - SonarCloud config"
    echo
    print_success "Happy coding! 🚀"
}

# Main execution
main() {
    echo -e "${GREEN}"
    echo "╔═══════════════════════════════════════════════════════════╗"
    echo "║                    Ktor Quality Setup                     ║"
    echo "║              Code Quality Tools Installation              ║"
    echo "╚═══════════════════════════════════════════════════════════╝"
    echo -e "${NC}"

    check_requirements
    create_directories
    install_precommit
    init_secrets_baseline
    create_convenience_scripts

    # Ask user if they want to run initial checks
    echo
    read -p "Do you want to run initial quality checks? (y/n): " -n 1 -r
    echo    # (move to a new line)
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        run_initial_checks
    else
        print_status "Skipping initial quality checks."
    fi

    display_completion_info
}

main "$@"
