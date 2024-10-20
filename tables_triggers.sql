CREATE TABLE Users (
    UserID SERIAL PRIMARY KEY,
    UserName VARCHAR(255) UNIQUE NOT NULL,
    Password VARCHAR(255) NOT NULL,
    UserRole VARCHAR(50) CHECK (UserRole IN ('admin', 'teacher', 'student')) NOT NULL
);

CREATE TABLE Subject (
  SubjectID SERIAL PRIMARY KEY,
  SubjectName VARCHAR(255) UNIQUE NOT NULL
);

CREATE TABLE Question (
    QuestionID SERIAL PRIMARY KEY,
    SubjectID INT NOT NULL REFERENCES Subject(SubjectID) ON DELETE CASCADE,
    QuestionText VARCHAR(255) UNIQUE NOT NULL,
    QuestionType VARCHAR(50) CHECK (QuestionType IN ('open', 'closed')) NOT NULL,
    QuestionDifficulty VARCHAR(50) CHECK (QuestionDifficulty IN ('easy', 'medium', 'hard')) NOT NULL
);

CREATE TABLE Answer (
    AnswerID SERIAL PRIMARY KEY,
    AnswerText VARCHAR(255) UNIQUE NOT NULL
);


CREATE TABLE Exam (
    ExamID SERIAL PRIMARY KEY,
    SubjectID INT NOT NULL REFERENCES Subject(SubjectID) ON DELETE CASCADE,
    UserID INT NOT NULL REFERENCES Users(UserID) ON DELETE CASCADE, 
    Title VARCHAR(255) NOT NULL,
    CreatedAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);


CREATE TABLE ExamQuestion (
    ExamID INT REFERENCES Exam(ExamID) ON DELETE CASCADE,
    QuestionID INT REFERENCES Question(QuestionID) ON DELETE CASCADE,
    CONSTRAINT pk_examQuestion PRIMARY KEY (ExamID, QuestionID)
);

CREATE TABLE QuestionAnswer (
    QuestionID INT REFERENCES Question(QuestionID) ON DELETE CASCADE,
    AnswerID INT REFERENCES Answer(AnswerID) ON DELETE CASCADE,
    IsCorrect BOOLEAN NOT NULL,
    CONSTRAINT pk_questionAnswer PRIMARY KEY (QuestionID, AnswerID)
);

CREATE TABLE StudentGrades (
    UserID INT REFERENCES Users(UserID) ON DELETE CASCADE,
    ExamID INT REFERENCES Exam(ExamID) ON DELETE CASCADE,
    Grade INT CHECK (Grade >= 0 AND Grade <= 100) NOT NULL,
    CONSTRAINT pk_studentGrade PRIMARY KEY (UserID, ExamID)
);


--delete question if there's no answers

CREATE OR REPLACE FUNCTION delete_question_if_no_answers() RETURNS TRIGGER AS $$
BEGIN
    -- Check if the question still has any linked answers
    IF NOT EXISTS (
        SELECT 1
        FROM questionanswer
        WHERE questionID = OLD.questionID
    ) THEN
        -- Delete the question if no answers are linked to it
        DELETE FROM question WHERE questionID = OLD.questionID;
    END IF;

    RETURN OLD;
END;
$$ LANGUAGE plpgsql;
CREATE TRIGGER after_questionanswer_delete
AFTER DELETE ON questionanswer
FOR EACH ROW
EXECUTE FUNCTION delete_question_if_no_answers();

--delete exam if there's no questions

CREATE OR REPLACE FUNCTION delete_exam_if_no_questions() RETURNS TRIGGER AS $$
BEGIN
    -- Check if the exam still has any linked questions
    IF NOT EXISTS (
        SELECT 1
        FROM examquestion
        WHERE examID = OLD.examID
    ) THEN
        -- Delete the exam if no questions are linked to it
        DELETE FROM exam WHERE examID = OLD.examID;
    END IF;

    RETURN OLD;
END;
$$ LANGUAGE plpgsql;
CREATE TRIGGER after_examquestion_delete
AFTER DELETE ON examquestion
FOR EACH ROW
EXECUTE FUNCTION delete_exam_if_no_questions();

--assert every entry in studentGrades contain a userID of a student

CREATE OR REPLACE FUNCTION check_student_role() RETURNS TRIGGER AS $$
BEGIN
    -- Check if the UserID has a UserRole of 'student'
    IF NOT EXISTS (
        SELECT 1
        FROM Users
        WHERE users.UserID = NEW.UserID
        AND users.UserRole = 'student'
    ) THEN
        -- Raise an exception if the UserRole is not 'student'
        RAISE EXCEPTION 'UserID % does not have a role of student', NEW."UserID";
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER before_studentgrades_insert
BEFORE INSERT ON StudentGrades
FOR EACH ROW
EXECUTE FUNCTION check_student_role();


--assert every entry in examquestion has examid and questionid of the same subject
CREATE OR REPLACE FUNCTION check_exam_question_subject_match()
    RETURNS TRIGGER AS $$
BEGIN
    -- Check if the subjects match
    IF (SELECT SubjectID FROM Exam WHERE ExamID = NEW.ExamID) !=
       (SELECT SubjectID FROM Question WHERE QuestionID = NEW.QuestionID) THEN
        RAISE EXCEPTION 'Exam subject and Question subject do not match';
    END IF;

    -- If subjects match, allow the insert
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_exam_question_subject_check
    BEFORE INSERT ON ExamQuestion
    FOR EACH ROW
EXECUTE FUNCTION check_exam_question_subject_match();


--assert every entry in exam has userID of a teacher

CREATE OR REPLACE FUNCTION check_teacher_role()
    RETURNS TRIGGER AS $$
BEGIN
    -- Check if the UserID corresponds to a 'teacher'
    IF (SELECT UserRole FROM Users WHERE UserID = NEW.UserID) != 'teacher' THEN
        RAISE EXCEPTION 'Only users with the role of teacher can create an exam';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER ensure_teacher_user
    BEFORE INSERT ON Exam
    FOR EACH ROW
EXECUTE FUNCTION check_teacher_role();

