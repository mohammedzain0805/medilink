# MediLink: Unified Cross-Hospital Medical Records Platform

PBCST 304 Project Phase I | Group 3 CSA (MEC)

MediLink is a Java Swing desktop application that lets authorised doctors see a patient's complete medical history across hospitals, with drug and allergy safety alerts, patient-controlled access, emergency access, and a full audit trail. Hospitals, drug data and notifications are **simulated in memory** for this project.

## Tech stack

| Tool | Use |
|---|---|
| Java 21 (JDK) | Object-oriented core |
| Java Swing | GUI (`JFrame`, `JPanel`, `JButton`, `JLabel`, tables, dialogs) |
| Apache Maven | Build and project configuration (`pom.xml`) |
| IntelliJ IDEA | Development environment |

## How to run

**In IntelliJ:** open the project folder, let Maven finish syncing, then run `com.medilink.Main`.

**From a terminal (JDK 21 and Maven installed):**
```
mvn compile
java -cp target/classes com.medilink.Main
```

## Demo accounts

| Role | Email | Password |
|---|---|---|
| Doctor (Cardiology) | meera@apollo.test | doctor123 |
| Doctor (Neurology) | ravi@medtrust.test | doctor123 |
| Patient | arun@mail.test | patient123 |
| Patient | riya@mail.test | patient123 |
| Admin | admin@medilink.test | admin123 |

Patients can also self-register from the login screen. Doctors and admins are created by an admin.

## Features by role

**Everyone:** secure login (salted PBKDF2 password hashes), audited login, logout, and failed login attempts.

**Doctor**
- Search records by record ID, by patient name + date of birth, or by hospital + record ID
- Load a patient's full cross-hospital history (local records plus every available hospital)
- View allergies, diagnoses and chronic conditions, prescriptions, test results and handoff notes
- Prescribe with real-time drug-interaction and allergy alerts (block, or override with an audit entry)
- Add handoff notes, request a second opinion from a colleague, send a record to a hospital
- Emergency access: reason required, 2-hour window, patient notified, every action tagged in the audit log

**Patient**
- View own records from all hospitals
- Grant or revoke each doctor's access (consent)
- See who accessed each record
- Set medication reminders and receive notifications

**Admin**
- View the full audit log
- List, add and remove user accounts (role is chosen at creation)
- Admins cannot read medical records (separation of duties)

## Access model

A doctor can open a record only if one of these is true:

1. The patient **consented** (the patient is in the doctor's `patientsUnderCare`).
2. A colleague **shared** the record (second opinion).
3. The doctor holds a valid, time-limited **emergency access** grant. Emergency access cannot be used to share or send records onward.

Patients can only open their own records. Admins cannot open any. Every view, denial, prescription, share, push, grant, revoke and emergency use is written to the audit log.

## Architecture

```
com.medilink
  Main                    wiring and demo data
  model/                  User (abstract), Admin, Doctor, Patient, Role, Allergy, MedicalRecord,
                          Diagnosis, Prescription, TestResult, AuditLog, Hospital, DrugAlert
  util/                   Validator, PasswordHasher
  repository/             UserRepository, RecordRepository, AuditRepository (+ InMemory versions)
  connector/              HospitalConnector (interface), AbstractHospitalConnector,
                          ApolloConnector, MedTrustConnector, GenericHL7Connector,
                          HospitalConnectorRegistry
  service/                AuthenticationService, UserService, RecordService, AuditService,
                          DrugInteractionService, NotificationService
  ui/                     MainFrame, LoginPanel, DoctorDashboard, PatientDashboard,
                          AdminPanel, RecordViewDialog
```

Rule: **UI classes only call services.** Services use repositories and connectors. The UI never touches storage, hashing or hospitals directly.

## OOP concepts and where to find them

| Concept | Where |
|---|---|
| **Encapsulation** | Private fields with getters/setters throughout `model/`. `User.setEmail` and `setFullName` validate. `MedicalRecord` returns unmodifiable lists. In `RecordService` the unchecked `collectHistory()` is private, so every public path goes through `authorise()` / `requireAccess()` and the audit log. |
| **Polymorphism (overriding)** | `HospitalConnector` implemented differently per hospital: `MedTrustConnector` rejects records with no diagnosis, `GenericHL7Connector` can go offline, `ApolloConnector` accepts everything. `User.hasAccessTo()` is overridden by `Doctor`, `Patient` and `Admin`. The core calls them all identically. |
| **Polymorphism (overloading)** | `RecordService.search()` has three overloads (record ID / name + DOB / hospital + record ID). `MedicalRecord` has two constructors. |
| **Abstraction** | The Swing UI calls single methods such as `getFullHistory()` and `prescribe()`. Cross-hospital fetching, merging, de-duplication, interaction checks and auditing are hidden behind them. Interfaces: `HospitalConnector` and the repositories. |
| **Inheritance** | `User` (abstract) is extended by `Admin`, `Doctor`, `Patient`. `AbstractHospitalConnector` is extended by the three hospital connectors. |

## Simulated parts (be upfront about these)

- Hospitals are in-memory connectors, not real HL7/FHIR endpoints.
- The drug-interaction and allergy table is a small hardcoded list. It is not clinical data and not medical advice.
- Notifications are an in-memory inbox, and "Send my reminders now" stands in for a scheduler.
- Data lives in memory and resets when the app closes.
- Demo passwords sit in `Main.java`. Real systems never keep credentials in source.

## Known limitations and future work

- Replace the `InMemory*` repositories with a database (the repository interfaces make this a local change).
- Real hospital calls would need background threads (`SwingWorker`) so the UI stays responsive.
- Revoking a doctor's consent does not remove record-level shares that doctor already made.
- Medication reminders have no real scheduler; "current medications" has no end dates.
- Emergency access can be recorded and reviewed but not technically prevented from misuse.
- Account removal keeps the patient's medical records (retention policy would need to be defined).
- No automated tests yet.
