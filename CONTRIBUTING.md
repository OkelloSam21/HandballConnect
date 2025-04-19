# Contributing to HandballConnect

First off, thank you for considering contributing to HandballConnect! It's people like you that make HandballConnect such a great tool for the handball community.

## Code of Conduct

By participating in this project, you are expected to uphold our Code of Conduct. Please report unacceptable behavior to [samuel](mailto:project_samuelokello929@gmail.com).

## How Can I Contribute?

### Reporting Bugs

This section guides you through submitting a bug report for HandballConnect. Following these guidelines helps maintainers and the community understand your report, reproduce the behavior, and find related reports.

* **Use the GitHub issue search** — check if the issue has already been reported.
* **Check if the issue has been fixed** — try to reproduce it using the latest `main` branch in the repository.
* **Isolate the problem** — ideally create a minimal reproduction.

### Feature Requests

This section guides you through submitting a feature request for HandballConnect, including completely new features and minor improvements to existing functionality.

* **Use the GitHub issue search** — check if the feature has already been requested.
* **Provide a clear and detailed explanation of the feature** you want and why it's important to add.
* **If possible, provide examples of how this feature would work** in similar applications.

### Pull Requests

* Fill in the required template
* Follow the Kotlin style guide
* Include appropriate tests
* Document new code based on the Documentation Styleguide
* End all files with a newline

## Styleguides

### Git Commit Messages

* Use the present tense ("Add feature" not "Added feature")
* Use the imperative mood ("Move cursor to..." not "Moves cursor to...")
* Limit the first line to 72 characters or less
* Reference issues and pull requests liberally after the first line
* Consider starting the commit message with an applicable emoji:
    * 🎨 `:art:` when improving the format/structure of the code
    * 🚀 `:rocket:` when improving performance
    * 📝 `:memo:` when writing docs
    * 🐛 `:bug:` when fixing a bug
    * 🔥 `:fire:` when removing code or files
    * ✅ `:white_check_mark:` when adding tests
    * 🔒 `:lock:` when dealing with security
    * ⬆️ `:arrow_up:` when upgrading dependencies
    * ⬇️ `:arrow_down:` when downgrading dependencies

### Kotlin Styleguide

* Follow the [Kotlin coding conventions](https://kotlinlang.org/docs/reference/coding-conventions.html)
* Use the [Android Kotlin Style Guide](https://developer.android.com/kotlin/style-guide)

### Documentation Styleguide

* Use [KDoc](https://kotlinlang.org/docs/reference/kotlin-doc.html) for code documentation
* Include code examples where appropriate

## Development Environment

### Project Setup

1. Fork and clone the repository
2. Install the required dependencies
3. Set up Firebase project and add `google-services.json` to the app folder
4. Run the app on an emulator or physical device

### Architecture Guidelines

When contributing to HandballConnect, please adhere to the MVVM architecture pattern:

* **Models**: Data classes that represent entities in the application
* **Views**: Compose UI components that display data
* **ViewModels**: Classes that prepare and manage data for the UI
* **Repositories**: Classes that handle data operations

### Testing

* Write unit tests for repository and ViewModel logic
* Use instrumentation tests for UI components
* Ensure all tests pass before submitting a pull request

## Additional Notes

### Issue and Pull Request Labels

This section lists the labels we use to help us track and manage issues and pull requests.

* `bug` - Issues that are bugs
* `enhancement` - Issues that are feature requests
* `documentation` - Issues or pull requests that affect documentation
* `good first issue` - Good for newcomers
* `help wanted` - Extra attention is needed
* `duplicate` - This issue or pull request already exists
* `wontfix` - This will not be worked on

## Attribution

This Contributing Guide is adapted from the open-source contributing guide templates from [GitHub](https://github.com/github/docs/blob/main/CONTRIBUTING.md).
