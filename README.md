
# Linux Auditd Configuration Management

This repository contains the automated configuration management suite for deploying, managing, and verifying **Auditd** (`audit.rules`) across Linux environments.

The project utilizes an **Ansible** role for configuration enforcement and a **Jenkins** pipeline to automate the deployment workflow safely.

---

## 💻 Repository Structure

* **`ansible-playbook/`**: Contains the master Ansible playbooks used to execute the auditd deployment across target hosts.
* `linux-auditd-configuration`: The main playbook entry point.


* **`roles/roles/update-auditd-configuration/tasks/`**: The core Ansible role responsible for pre-requisite validation, file distribution, and service management.
* **`Jenkinsfile` (or `*.groovy`)**: The Groovy-based pipeline script that orchestrates the execution of the Ansible playbooks via Jenkins CI/CD.
* **`auditd.rules`** :  config file 

---

## 🛠️ How it Works

The deployment follows a structured, safety-first workflow to ensure that a broken or misconfigured `auditd.rules` file never leaves a system unmonitored or broken.

```
[ Jenkins (Groovy) ] ──> [ Ansible Playbook ] ──> [ Role: Pre-checks ] ──> [ Apply & Restart ]

```

### 1. CI/CD Orchestration (Jenkins & Groovy)

The pipeline is defined via a **Groovy file** (Jenkinsfile). It automates the following stages:

* Linting and syntax checking of the Ansible code.
* Target inventory selection.
* Triggering the Ansible playbook execution.

### 2. Safeguards & Requirements Verification (Ansible Role)

Before making any changes to the active system logging, the **Ansible role** performs critical pre-requisite checks on the target host:

* **OS Compatibility**: Verifies the target Linux distribution is supported.
* **Auditd Presence**: Checks if the `auditd` package is installed or prepares to install it.
* **Syntax & Rule Validation**: Validates that the new `audit.rules` template contains no conflicting or corrupt rules before replacing the production file.
* **Service Status**: Ensures dependent system services are running as expected.

### 3. Deployment & Enforcement

Only after all pre-requisite checks pass successfully does the role:

1. Backup the existing configuration.
2. Deploy the new updated `auditd.rules` file.
3. Gracefully restart/reload the `auditd` service to apply the new security rules.

---

## 🚀 Usage

### Running Locally via Ansible

To trigger the deployment manually from the command line, navigate to the `ansible-playbook` folder and run:

```bash
ansible-playbook -i inventory.ini linux-auditd-configuration

```

### Running via Jenkins

1. Create a new Pipeline job in Jenkins.
2. Point the SCM configuration to this repository.
3. Configure it to read the provided **Groovy file** (`Jenkinsfile`).
4. Run the build.

> ⚠️ **Important**: Ensure that the Jenkins agent executing the job has the appropriate SSH credentials and sudo privileges required by Ansible to manage the `auditd` service on target nodes.

---

Feel free to customize the filenames (like replacing `linux-auditd-configuration.yml` with your actual playbook name) to match your exact setup! How do you handle the syntax validation step for the audit rules before they get deployed?
