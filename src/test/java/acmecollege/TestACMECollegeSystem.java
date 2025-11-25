/**
 * File:  TestACMECollegeSystem.java
 * Course materials (23W) CST 8277
 * Teddy Yap
 * (Original Author) Mike Norman
 *
 * @date 2020 10
 *
 * Updated by: Group NN
 *   Lucas, Subhechha, David, Abhiram
 */
package acmecollege;

import static acmecollege.utility.MyConstants.APPLICATION_API_VERSION;
import static acmecollege.utility.MyConstants.APPLICATION_CONTEXT_ROOT;
import static acmecollege.utility.MyConstants.DEFAULT_ADMIN_USER;
import static acmecollege.utility.MyConstants.DEFAULT_ADMIN_USER_PASSWORD;
import static acmecollege.utility.MyConstants.DEFAULT_USER;
import static acmecollege.utility.MyConstants.DEFAULT_USER_PASSWORD;
import static acmecollege.utility.MyConstants.STUDENT_RESOURCE_NAME;
import static acmecollege.utility.MyConstants.COURSE_RESOURCE_NAME;
import static acmecollege.utility.MyConstants.PROFESSOR_SUBRESOURCE_NAME;
import static acmecollege.utility.MyConstants.STUDENT_CLUB_RESOURCE_NAME;
import static acmecollege.utility.MyConstants.CLUB_MEMBERSHIP_RESOURCE_NAME;
import static acmecollege.utility.MyConstants.MEMBERSHIP_CARD_RESOURCE_NAME;
import static acmecollege.utility.MyConstants.COURSE_REGISTRATION_RESOURCE_NAME;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;

import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.util.List;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.authentication.HttpAuthenticationFeature;
import org.glassfish.jersey.logging.LoggingFeature;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;

import acmecollege.entity.AcademicStudentClub;
import acmecollege.entity.ClubMembership;
import acmecollege.entity.Course;
import acmecollege.entity.MembershipCard;
import acmecollege.entity.NonAcademicStudentClub;
import acmecollege.entity.Professor;
import acmecollege.entity.Student;
import acmecollege.entity.StudentClub;

@SuppressWarnings("unused")

@TestMethodOrder(MethodOrderer.MethodName.class)
public class TestACMECollegeSystem {
    private static final Class<?> _thisClaz = MethodHandles.lookup().lookupClass();
    private static final Logger logger = LogManager.getLogger(_thisClaz);

    static final String HTTP_SCHEMA = "http";
    static final String HOST = "localhost";
    static final int PORT = 8080;

    // Test fixture(s)
    static URI uri;
    static HttpAuthenticationFeature adminAuth;
    static HttpAuthenticationFeature userAuth;

    @BeforeAll
    public static void oneTimeSetUp() throws Exception {
        logger.debug("oneTimeSetUp");
        uri = UriBuilder
            .fromUri(APPLICATION_CONTEXT_ROOT + APPLICATION_API_VERSION)
            .scheme(HTTP_SCHEMA)
            .host(HOST)
            .port(PORT)
            .build();
        adminAuth = HttpAuthenticationFeature.basic(DEFAULT_ADMIN_USER, DEFAULT_ADMIN_USER_PASSWORD);
        userAuth = HttpAuthenticationFeature.basic(DEFAULT_USER, DEFAULT_USER_PASSWORD);
    }

    protected WebTarget webTarget;
    @BeforeEach
    public void setUp() {
        Client client = ClientBuilder.newClient(
            new ClientConfig().register(MyObjectMapperProvider.class).register(new LoggingFeature()));
        webTarget = client.target(uri);
    }

    // ===== STUDENT TESTS =====
    
    @Test
    public void test01_all_students_with_adminrole() throws JsonMappingException, JsonProcessingException {
        Response response = webTarget
            .register(adminAuth)
            .path(STUDENT_RESOURCE_NAME)
            .request()
            .get();
        assertThat(response.getStatus(), is(200));
        List<Student> students = response.readEntity(new GenericType<List<Student>>(){});
        assertThat(students, is(not(empty())));
        assertThat(students, hasSize(greaterThanOrEqualTo(1)));
    }

    @Test
    public void test02_all_students_with_userrole_forbidden() {
        Response response = webTarget
            .register(userAuth)
            .path(STUDENT_RESOURCE_NAME)
            .request()
            .get();
        assertThat(response.getStatus(), is(403));
    }

    @Test
    public void test03_get_student_by_id_with_adminrole() {
        Response response = webTarget
            .register(adminAuth)
            .path(STUDENT_RESOURCE_NAME + "/1")
            .request()
            .get();
        assertThat(response.getStatus(), is(200));
        Student student = response.readEntity(Student.class);
        assertThat(student, is(notNullValue()));
        assertThat(student.getFirstName(), is("John"));
        assertThat(student.getLastName(), is("Smith"));
    }

    @Test
    public void test04_get_student_by_id_with_userrole_own_student() {
        Response response = webTarget
            .register(userAuth)
            .path(STUDENT_RESOURCE_NAME + "/1")
            .request()
            .get();
        assertThat(response.getStatus(), is(200));
        Student student = response.readEntity(Student.class);
        assertThat(student, is(notNullValue()));
    }

    @Test
    public void test05_get_student_by_id_with_userrole_other_student_forbidden() {
        Response response = webTarget
            .register(userAuth)
            .path(STUDENT_RESOURCE_NAME + "/999")
            .request()
            .get();
        assertThat(response.getStatus(), is(403));
    }

    @Test
    public void test06_get_student_not_found() {
        Response response = webTarget
            .register(adminAuth)
            .path(STUDENT_RESOURCE_NAME + "/999")
            .request()
            .get();
        assertThat(response.getStatus(), is(404));
    }

    @Test
    public void test07_all_students_no_auth_unauthorized() {
        Response response = webTarget
            .path(STUDENT_RESOURCE_NAME)
            .request()
            .get();
        assertThat(response.getStatus(), is(401));
    }

    @Test
    public void test08_add_student_with_adminrole() {
        Student newStudent = new Student();
        newStudent.setFirstName("Test");
        newStudent.setLastName("Student");
        
        Response response = webTarget
            .register(adminAuth)
            .path(STUDENT_RESOURCE_NAME)
            .request()
            .post(Entity.entity(newStudent, MediaType.APPLICATION_JSON));
        assertThat(response.getStatus(), is(200));
    }

    @Test
    public void test09_add_student_with_userrole_forbidden() {
        Student newStudent = new Student();
        newStudent.setFirstName("Test");
        newStudent.setLastName("User");
        
        Response response = webTarget
            .register(userAuth)
            .path(STUDENT_RESOURCE_NAME)
            .request()
            .post(Entity.entity(newStudent, MediaType.APPLICATION_JSON));
        assertThat(response.getStatus(), is(403));
    }

    // ===== COURSE TESTS =====

    @Test
    public void test10_all_courses_with_adminrole() {
        Response response = webTarget
            .register(adminAuth)
            .path(COURSE_RESOURCE_NAME)
            .request()
            .get();
        assertThat(response.getStatus(), is(200));
        List<Course> courses = response.readEntity(new GenericType<List<Course>>(){});
        assertThat(courses, is(not(empty())));
    }

    @Test
    public void test11_all_courses_with_userrole() {
        Response response = webTarget
            .register(userAuth)
            .path(COURSE_RESOURCE_NAME)
            .request()
            .get();
        assertThat(response.getStatus(), is(200));
    }

    @Test
    public void test12_get_course_by_id_with_adminrole() {
        Response response = webTarget
            .register(adminAuth)
            .path(COURSE_RESOURCE_NAME + "/1")
            .request()
            .get();
        assertThat(response.getStatus(), is(200));
        Course course = response.readEntity(Course.class);
        assertThat(course, is(notNullValue()));
    }

    @Test
    public void test13_add_course_with_adminrole() {
        Course newCourse = new Course();
        newCourse.setCourseCode("CST9999");
        newCourse.setCourseTitle("Test Course");
        newCourse.setYear(2024);
        newCourse.setSemester("WINTER");
        newCourse.setCreditUnits(3);
        newCourse.setOnline((byte) 0);
        
        Response response = webTarget
            .register(adminAuth)
            .path(COURSE_RESOURCE_NAME)
            .request()
            .post(Entity.entity(newCourse, MediaType.APPLICATION_JSON));
        assertThat(response.getStatus(), is(200));
    }

    @Test
    public void test14_add_course_with_userrole_forbidden() {
        Course newCourse = new Course();
        newCourse.setCourseCode("CST0000");
        newCourse.setCourseTitle("Forbidden Course");
        newCourse.setYear(2024);
        newCourse.setSemester("FALL");
        newCourse.setCreditUnits(3);
        newCourse.setOnline((byte) 0);
        
        Response response = webTarget
            .register(userAuth)
            .path(COURSE_RESOURCE_NAME)
            .request()
            .post(Entity.entity(newCourse, MediaType.APPLICATION_JSON));
        assertThat(response.getStatus(), is(403));
    }

    @Test
    public void test15_all_courses_no_auth_unauthorized() {
        Response response = webTarget
            .path(COURSE_RESOURCE_NAME)
            .request()
            .get();
        assertThat(response.getStatus(), is(401));
    }

    // ===== PROFESSOR TESTS =====

    @Test
    public void test16_all_professors_with_adminrole() {
        Response response = webTarget
            .register(adminAuth)
            .path(PROFESSOR_SUBRESOURCE_NAME)
            .request()
            .get();
        assertThat(response.getStatus(), is(200));
        List<Professor> professors = response.readEntity(new GenericType<List<Professor>>(){});
        assertThat(professors, is(not(empty())));
    }

    @Test
    public void test17_all_professors_with_userrole() {
        Response response = webTarget
            .register(userAuth)
            .path(PROFESSOR_SUBRESOURCE_NAME)
            .request()
            .get();
        assertThat(response.getStatus(), is(200));
    }

    @Test
    public void test18_get_professor_by_id() {
        Response response = webTarget
            .register(adminAuth)
            .path(PROFESSOR_SUBRESOURCE_NAME + "/1")
            .request()
            .get();
        assertThat(response.getStatus(), is(200));
        Professor professor = response.readEntity(Professor.class);
        assertThat(professor, is(notNullValue()));
    }

    @Test
    public void test19_add_professor_with_adminrole() {
        Professor newProfessor = new Professor();
        newProfessor.setFirstName("Test");
        newProfessor.setLastName("Professor");
        newProfessor.setDepartment("Computer Science");
        
        Response response = webTarget
            .register(adminAuth)
            .path(PROFESSOR_SUBRESOURCE_NAME)
            .request()
            .post(Entity.entity(newProfessor, MediaType.APPLICATION_JSON));
        assertThat(response.getStatus(), is(200));
    }

    @Test
    public void test20_add_professor_with_userrole_forbidden() {
        Professor newProfessor = new Professor();
        newProfessor.setFirstName("Forbidden");
        newProfessor.setLastName("Professor");
        newProfessor.setDepartment("Engineering");
        
        Response response = webTarget
            .register(userAuth)
            .path(PROFESSOR_SUBRESOURCE_NAME)
            .request()
            .post(Entity.entity(newProfessor, MediaType.APPLICATION_JSON));
        assertThat(response.getStatus(), is(403));
    }

    // ===== STUDENT CLUB TESTS =====

    @Test
    public void test21_all_student_clubs_no_auth() {
        Response response = webTarget
            .path(STUDENT_CLUB_RESOURCE_NAME)
            .request()
            .get();
        // Any user can retrieve the list of StudentClub
        assertThat(response.getStatus(), is(200));
    }

    @Test
    public void test22_all_student_clubs_with_adminrole() {
        Response response = webTarget
            .register(adminAuth)
            .path(STUDENT_CLUB_RESOURCE_NAME)
            .request()
            .get();
        assertThat(response.getStatus(), is(200));
        List<StudentClub> clubs = response.readEntity(new GenericType<List<StudentClub>>(){});
        assertThat(clubs, is(not(empty())));
    }

    @Test
    public void test23_get_student_club_by_id() {
        Response response = webTarget
            .register(adminAuth)
            .path(STUDENT_CLUB_RESOURCE_NAME + "/1")
            .request()
            .get();
        assertThat(response.getStatus(), is(200));
    }

    @Test
    public void test24_add_student_club_with_adminrole() {
        AcademicStudentClub newClub = new AcademicStudentClub();
        newClub.setName("Test Academic Club " + System.currentTimeMillis());
        
        Response response = webTarget
            .register(adminAuth)
            .path(STUDENT_CLUB_RESOURCE_NAME)
            .request()
            .post(Entity.entity(newClub, MediaType.APPLICATION_JSON));
        assertThat(response.getStatus(), is(200));
    }

    @Test
    public void test25_add_student_club_with_userrole_forbidden() {
        NonAcademicStudentClub newClub = new NonAcademicStudentClub();
        newClub.setName("Forbidden Club " + System.currentTimeMillis());
        
        Response response = webTarget
            .register(userAuth)
            .path(STUDENT_CLUB_RESOURCE_NAME)
            .request()
            .post(Entity.entity(newClub, MediaType.APPLICATION_JSON));
        assertThat(response.getStatus(), is(403));
    }

    @Test
    public void test26_delete_student_club_with_userrole_forbidden() {
        Response response = webTarget
            .register(userAuth)
            .path(STUDENT_CLUB_RESOURCE_NAME + "/1")
            .request()
            .delete();
        assertThat(response.getStatus(), is(403));
    }

    // ===== CLUB MEMBERSHIP TESTS =====

    @Test
    public void test27_all_club_memberships_with_adminrole() {
        Response response = webTarget
            .register(adminAuth)
            .path(CLUB_MEMBERSHIP_RESOURCE_NAME)
            .request()
            .get();
        assertThat(response.getStatus(), is(200));
    }

    @Test
    public void test28_all_club_memberships_with_userrole() {
        Response response = webTarget
            .register(userAuth)
            .path(CLUB_MEMBERSHIP_RESOURCE_NAME)
            .request()
            .get();
        assertThat(response.getStatus(), is(200));
    }

    @Test
    public void test29_get_club_membership_by_id() {
        Response response = webTarget
            .register(adminAuth)
            .path(CLUB_MEMBERSHIP_RESOURCE_NAME + "/1")
            .request()
            .get();
        assertThat(response.getStatus(), is(200));
    }

    @Test
    public void test30_delete_club_membership_with_userrole_forbidden() {
        Response response = webTarget
            .register(userAuth)
            .path(CLUB_MEMBERSHIP_RESOURCE_NAME + "/1")
            .request()
            .delete();
        assertThat(response.getStatus(), is(403));
    }

    // ===== MEMBERSHIP CARD TESTS =====

    @Test
    public void test31_all_membership_cards_with_adminrole() {
        Response response = webTarget
            .register(adminAuth)
            .path(MEMBERSHIP_CARD_RESOURCE_NAME)
            .request()
            .get();
        assertThat(response.getStatus(), is(200));
    }

    @Test
    public void test32_all_membership_cards_with_userrole_forbidden() {
        Response response = webTarget
            .register(userAuth)
            .path(MEMBERSHIP_CARD_RESOURCE_NAME)
            .request()
            .get();
        assertThat(response.getStatus(), is(403));
    }

    @Test
    public void test33_get_membership_card_by_id_with_adminrole() {
        Response response = webTarget
            .register(adminAuth)
            .path(MEMBERSHIP_CARD_RESOURCE_NAME + "/1")
            .request()
            .get();
        assertThat(response.getStatus(), is(200));
    }

    @Test
    public void test34_get_own_membership_card_with_userrole() {
        Response response = webTarget
            .register(userAuth)
            .path(MEMBERSHIP_CARD_RESOURCE_NAME + "/1")
            .request()
            .get();
        assertThat(response.getStatus(), is(200));
    }

    @Test
    public void test35_delete_membership_card_with_userrole_forbidden() {
        Response response = webTarget
            .register(userAuth)
            .path(MEMBERSHIP_CARD_RESOURCE_NAME + "/1")
            .request()
            .delete();
        assertThat(response.getStatus(), is(403));
    }

    // ===== COURSE REGISTRATION TESTS =====

    @Test
    public void test36_all_course_registrations_with_adminrole() {
        Response response = webTarget
            .register(adminAuth)
            .path(COURSE_REGISTRATION_RESOURCE_NAME)
            .request()
            .get();
        assertThat(response.getStatus(), is(200));
    }

    @Test
    public void test37_all_course_registrations_with_userrole() {
        Response response = webTarget
            .register(userAuth)
            .path(COURSE_REGISTRATION_RESOURCE_NAME)
            .request()
            .get();
        assertThat(response.getStatus(), is(200));
    }

    @Test
    public void test38_get_course_registration_by_id() {
        Response response = webTarget
            .register(adminAuth)
            .path(COURSE_REGISTRATION_RESOURCE_NAME + "/1/1")
            .request()
            .get();
        assertThat(response.getStatus(), is(200));
    }

    @Test
    public void test39_delete_course_registration_with_userrole_forbidden() {
        Response response = webTarget
            .register(userAuth)
            .path(COURSE_REGISTRATION_RESOURCE_NAME + "/1/1")
            .request()
            .delete();
        assertThat(response.getStatus(), is(403));
    }

    // ===== ADDITIONAL NEGATIVE TESTS =====

    @Test
    public void test40_get_nonexistent_course() {
        Response response = webTarget
            .register(adminAuth)
            .path(COURSE_RESOURCE_NAME + "/999")
            .request()
            .get();
        assertThat(response.getStatus(), is(404));
    }

    @Test
    public void test41_get_nonexistent_professor() {
        Response response = webTarget
            .register(adminAuth)
            .path(PROFESSOR_SUBRESOURCE_NAME + "/999")
            .request()
            .get();
        assertThat(response.getStatus(), is(404));
    }

    @Test
    public void test42_delete_professor_with_userrole_forbidden() {
        Response response = webTarget
            .register(userAuth)
            .path(PROFESSOR_SUBRESOURCE_NAME + "/1")
            .request()
            .delete();
        assertThat(response.getStatus(), is(403));
    }

    @Test
    public void test43_delete_course_with_userrole_forbidden() {
        Response response = webTarget
            .register(userAuth)
            .path(COURSE_RESOURCE_NAME + "/1")
            .request()
            .delete();
        assertThat(response.getStatus(), is(403));
    }

    @Test
    public void test44_update_student_club_with_adminrole() {
        AcademicStudentClub updatedClub = new AcademicStudentClub();
        updatedClub.setName("Updated Club Name");
        
        Response response = webTarget
            .register(adminAuth)
            .path(STUDENT_CLUB_RESOURCE_NAME + "/1")
            .request()
            .put(Entity.entity(updatedClub, MediaType.APPLICATION_JSON));
        assertThat(response.getStatus(), is(200));
    }

    @Test
    public void test45_professors_no_auth_unauthorized() {
        Response response = webTarget
            .path(PROFESSOR_SUBRESOURCE_NAME)
            .request()
            .get();
        assertThat(response.getStatus(), is(401));
    }
}