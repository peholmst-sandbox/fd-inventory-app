# MCP Servers Summary

## 1. IDE (`mcp__ide__`)
Tools for interacting with the VS Code IDE:
- **getDiagnostics** - Get language diagnostics (errors, warnings) from VS Code
- **executeCode** - Execute Python code in a Jupyter kernel

## 2. Vaadin (`mcp__Vaadin__`)
Comprehensive Vaadin framework documentation and API tools:
- **get_vaadin_primer** - Get an up-to-date primer on modern Vaadin development
- **search_vaadin_docs** - Search documentation with hybrid semantic + keyword search
- **get_full_document** - Retrieve complete documentation pages
- **get_vaadin_version** - Get the latest stable Vaadin version
- **get_components_by_version** - List all components for a specific version
- **get_component_java_api** - Get Java API docs for a component
- **get_component_react_api** - Get React API docs for a component
- **get_component_web_component_api** - Get Web Component/TypeScript API docs
- **get_component_styling** - Get styling/theming documentation

## 3. Playwright (`mcp__playwright__`)
Browser automation for testing and web interaction:
- **Navigation**: `browser_navigate`, `browser_navigate_back`, `browser_tabs`
- **Interaction**: `browser_click`, `browser_type`, `browser_hover`, `browser_drag`, `browser_select_option`, `browser_fill_form`, `browser_press_key`
- **Inspection**: `browser_snapshot`, `browser_take_screenshot`, `browser_console_messages`, `browser_network_requests`
- **Utilities**: `browser_evaluate`, `browser_file_upload`, `browser_handle_dialog`, `browser_wait_for`, `browser_resize`, `browser_close`, `browser_install`, `browser_run_code`

## 4. Spring Docs (`mcp__spring-docs__`)
Spring Boot and Spring ecosystem documentation:
- **search_spring_docs** - Search Spring Boot documentation
- **search_spring_projects** - Search all Spring projects
- **get_spring_project** - Get details of a specific project
- **get_all_spring_guides** - List available Spring guides
- **get_spring_guide** - Get content of a specific guide
- **get_spring_reference** - Get reference documentation sections
- **search_spring_concepts** - Search concepts with explanations
- **search_spring_ecosystem** - Search across the entire ecosystem
- **get_spring_tutorial** - Get step-by-step tutorials
- **compare_spring_versions** - Compare features between versions
- **get_spring_best_practices** - Get development best practices
- **diagnose_spring_issues** - Troubleshoot common issues

## 5. Java (`mcp__java__`)
Maven Central and Javadoc tools:
- **get_latest_version** - Get the latest version of a Maven artifact
- **get_javadoc_content_list** - List contents of a javadoc jar
- **get_javadoc_symbol_contents** - Get documentation for a specific symbol
- **symbol_to_artifact** - Find the Maven artifact for a class/package name
