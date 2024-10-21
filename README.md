# Exam Management System

This project consists of a **Java** client application that connects to a **PostgreSQL** database to manage an exam system. The system allows administrators, teachers, and students to interact with the database by adding and deleting questions, answers, exams, and users. Teachers can assign grades to students, and students can check their GPA through the system.

The project demonstrates advanced knowledge in SQL, including complex multi-table joins, the creation and management of triggers for data integrity, and efficient database design. It also showcases strong proficiency in Java programming, including usage of JDBC for database connectivity, proper handling of SQL exceptions, and configuration management using external files like `.properties` for secure and flexible connection settings.

## Table of Contents

- [Features](#features)
- [Technologies](#technologies)
- [Setup and Configuration](#setup-and-configuration)
- [Database Structure](#database-structure)
- [Triggers](#triggers)

## Features

- **Question Management**: Add and delete questions (open or closed) and answers for exams.
- **Exam Management**: Create, edit, and delete exams from the system.
- **User Management**: Add users, including administrators, teachers, and students.
- **Grade Management**: Teachers can assign grades to students after exams.
- **GPA Calculation**: Students can check their GPA, which is dynamically calculated based on their exam results.

## Technologies

- **Java**
- **PostgreSQL**
- **JDBC Driver** for PostgreSQL

## Setup and Configuration

1. **Configure Database Connection:**

   Create a `config.properties` file in the root directory of the project, with the following structure:
   ```properties
   db.url=jdbc:postgresql://localhost:5432/examdb
   db.user=yourusername
   db.password=yourpassword
   ```

2. **Add PostgreSQL JDBC Driver:**

   Make sure to use the provided jar file as a library for the project.

## Database Structure

![tables](https://github.com/user-attachments/assets/96bbd5cb-b7c3-44f2-b155-181bb9f57516)


The following tables are used in this project:

1. **Users**: Stores user information (admin, teachers, students).
   - `UserID`: Primary key.
   - `UserName`: Unique username.
   - `Password`: User password.
   - `UserRole`: Specifies the role (`admin`, `teacher`, `student`).

2. **Subject**: Stores subjects for exams.
   - `SubjectID`: Primary key.
   - `SubjectName`: Name of the subject.

3. **Question**: Stores exam questions.
   - `QuestionID`: Primary key.
   - `SubjectID`: Foreign key referencing `Subject`.
   - `QuestionText`: The text of the question.
   - `QuestionType`: Specifies whether the question is `open` or `closed`.
   - `QuestionDifficulty`: The difficulty level (`easy`, `medium`, `hard`).

4. **Answer**: Stores answers for closed questions.
   - `AnswerID`: Primary key.
   - `AnswerText`: Text of the answer.

5. **Exam**: Stores exam information.
   - `ExamID`: Primary key.
   - `SubjectID`: Foreign key referencing `Subject`.
   - `UserID`: Foreign key referencing the teacher who created the exam.
   - `Title`: Title of the exam.
   - `CreatedAt`: Timestamp of exam creation.

6. **ExamQuestion**: Stores questions associated with exams.
   - `ExamID`: Foreign key referencing `Exam`.
   - `QuestionID`: Foreign key referencing `Question`.

7. **QuestionAnswer**: Stores correct or incorrect answers for questions.
   - `QuestionID`: Foreign key referencing `Question`.
   - `AnswerID`: Foreign key referencing `Answer`.
   - `IsCorrect`: Boolean flag indicating if the answer is correct.

8. **StudentGrades**: Stores student grades for exams.
   - `UserID`: Foreign key referencing the student.
   - `ExamID`: Foreign key referencing `Exam`.
   - `Grade`: Grade between 0 and 100.

## Triggers

The project includes several database triggers to ensure data integrity:

1. **Delete Question if No Answers**: Automatically deletes a question if no answers are associated with it.
   
2. **Student Role Validation**: Ensures that only users with the `student` role can have grades assigned.
   - Trigger: `before_studentgrades_insert`

3. **Exam-Question Subject Match**: Ensures that the subject of an exam and its associated questions match.
   - Trigger: `trg_exam_question_subject_check`

4. **Teacher Role Validation for Exam Creation**: Ensures that only users with the `teacher` role can create exams.
   - Trigger: `ensure_teacher_user`
