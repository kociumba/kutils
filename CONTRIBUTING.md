# Contributing to kutils

Thank you for your interest in contributing to **kutils**! This document outlines the basic steps for getting started and submitting contributions.

## How to Contribute

The recommended way to contribute is by creating a **pull request (PR)** with your changes. Before a PR can be merged, all GitHub Actions workflows must pass successfully.

## Setting Up Your Development Environment

kutils is a **Kotlin/Java** project and uses **Gradle** for building and dependency management. To get started:

1. **Clone the repository:**

   ```sh
   git clone https://github.com/kociumba/kutils.git
   cd kutils
   ```

2. **Ensure you have Java 21 installed (personally i recommend graalvm for performance)**

3. **Set up GitHub credentials** (required for resolving dependencies from GitHub Packages). You need to provide your GitHub username and a personal access token with `read:packages` scope:

   - Set credentials as environment variables:
     - **Linux/macOS:**
       ```sh
       export GITHUB_ACTOR="your-username"
       export GITHUB_TOKEN="your-personal-access-token"
       ```
     - **Windows (PowerShell):**
       ```powershel
       $env:GITHUB_ACTOR="your-username"
       $env:GITHUB_TOKEN="your-personal-access-token"
       ```

   Alternatively, add them to your Gradle properties file (`~/.gradle/gradle.properties`). **Do not commit these credentials for security reasons:**

   ```properties
   gpr.user=your-username
   gpr.key=your-personal-access-token
   ```

4. **Build and run the project:**

   ```sh
   ./gradlew build
   ```

   This will download dependencies and compile the mod into `./build/libs/kutils-version_number.jar`.

## Creating a Branch or Fork

Before making changes, you need to create a **new branch** or a **fork** of the repository. This allows you to:

- Keep your changes separate from the `main` branch.
- Run GitHub Actions checks on your fork or branch before submitting a PR.
- Work on multiple contributions simultaneously without conflicts.

To create a new branch:

```sh
git checkout -b my-feature-branch
```

To create a fork, go to the [GitHub repository](https://github.com/kociumba/kutils) and click **Fork**.

Once your changes are ready, push your branch and open a pull request.

## Pull Request Guidelines

- Follow the existing **code style** used in the project.
- Provide clear commit messages explaining your changes.
- Keep PRs focused on a single feature or bug fix whenever possible.
- If your PR introduces a new feature, consider adding documentation or comments explaining its usage.

## Reporting Issues

If you find a bug or have a feature request, please open an issue in the [GitHub Issues](https://github.com/kociumba/kutils/issues). Try to use one of the issue templates provided and include as much relevant information as possible.

## License

By contributing to kutils, you agree that your contributions will be licensed under the same license as the project.

Happy coding!

