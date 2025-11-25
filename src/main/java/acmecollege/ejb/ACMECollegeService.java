/**
 * File:  ACMEColegeService.java
 * Course materials (23W) CST 8277
 *
 * @author Teddy Yap
 * @author Shariar (Shawn) Emami
 * @author (original) Mike Norman
 * 
 * Updated by:  Group NN
 *   Lucas, Subhechha, David, Abhiram
 *
 */
package acmecollege.ejb;

import static acmecollege.entity.StudentClub.ALL_STUDENT_CLUBS_QUERY_NAME;
import static acmecollege.entity.StudentClub.SPECIFIC_STUDENT_CLUB_QUERY_NAME;
import static acmecollege.entity.StudentClub.IS_DUPLICATE_QUERY_NAME;
import static acmecollege.entity.Student.ALL_STUDENTS_QUERY_NAME;
import static acmecollege.utility.MyConstants.DEFAULT_KEY_SIZE;
import static acmecollege.utility.MyConstants.DEFAULT_PROPERTY_ALGORITHM;
import static acmecollege.utility.MyConstants.DEFAULT_PROPERTY_ITERATIONS;
import static acmecollege.utility.MyConstants.DEFAULT_SALT_SIZE;
import static acmecollege.utility.MyConstants.DEFAULT_USER_PASSWORD;
import static acmecollege.utility.MyConstants.DEFAULT_USER_PREFIX;
import static acmecollege.utility.MyConstants.PARAM1;
import static acmecollege.utility.MyConstants.PROPERTY_ALGORITHM;
import static acmecollege.utility.MyConstants.PROPERTY_ITERATIONS;
import static acmecollege.utility.MyConstants.PROPERTY_KEY_SIZE;
import static acmecollege.utility.MyConstants.PROPERTY_SALT_SIZE;
import static acmecollege.utility.MyConstants.PU_NAME;
import static acmecollege.utility.MyConstants.USER_ROLE;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ejb.Singleton;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import javax.security.enterprise.identitystore.Pbkdf2PasswordHash;
import javax.transaction.Transactional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import acmecollege.entity.ClubMembership;
import acmecollege.entity.Course;
import acmecollege.entity.CourseRegistration;
import acmecollege.entity.CourseRegistrationPK;
import acmecollege.entity.MembershipCard;
import acmecollege.entity.Professor;
import acmecollege.entity.SecurityRole;
import acmecollege.entity.SecurityUser;
import acmecollege.entity.Student;
import acmecollege.entity.StudentClub;

@SuppressWarnings("unused")

/**
 * Stateless Singleton EJB Bean - ACMECollegeService
 */
@Singleton
public class ACMECollegeService implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private static final Logger LOG = LogManager.getLogger();
    
    @PersistenceContext(name = PU_NAME)
    protected EntityManager em;
    
    @Inject
    protected Pbkdf2PasswordHash pbAndjPasswordHash;

    public List<Student> getAllStudents() {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Student> cq = cb.createQuery(Student.class);
        cq.select(cq.from(Student.class));
        return em.createQuery(cq).getResultList();
    }

    public Student getStudentById(int id) {
        return em.find(Student.class, id);
    }

    @Transactional
    public Student persistStudent(Student newStudent) {
        em.persist(newStudent);
        return newStudent;
    }

    @Transactional
    public void buildUserForNewStudent(Student newStudent) {
        SecurityUser userForNewStudent = new SecurityUser();
        userForNewStudent.setUsername(
            DEFAULT_USER_PREFIX + "_" + newStudent.getFirstName() + "." + newStudent.getLastName());
        Map<String, String> pbAndjProperties = new HashMap<>();
        pbAndjProperties.put(PROPERTY_ALGORITHM, DEFAULT_PROPERTY_ALGORITHM);
        pbAndjProperties.put(PROPERTY_ITERATIONS, DEFAULT_PROPERTY_ITERATIONS);
        pbAndjProperties.put(PROPERTY_SALT_SIZE, DEFAULT_SALT_SIZE);
        pbAndjProperties.put(PROPERTY_KEY_SIZE, DEFAULT_KEY_SIZE);
        pbAndjPasswordHash.initialize(pbAndjProperties);
        String pwHash = pbAndjPasswordHash.generate(DEFAULT_USER_PASSWORD.toCharArray());
        userForNewStudent.setPwHash(pwHash);
        userForNewStudent.setStudent(newStudent);
        SecurityRole userRole = em.createNamedQuery(SecurityRole.ROLE_BY_NAME_QUERY, SecurityRole.class)
            .setParameter(PARAM1, USER_ROLE)
            .getSingleResult();
        userForNewStudent.getRoles().add(userRole);
        userRole.getUsers().add(userForNewStudent);
        em.persist(userForNewStudent);
    }

    @Transactional
    public Professor setProfessorForStudentCourse(int studentId, int courseId, Professor newProfessor) {
        Student studentToBeUpdated = em.find(Student.class, studentId);
        if (studentToBeUpdated != null) { // Student exists
            Set<CourseRegistration> courseRegistrations = studentToBeUpdated.getCourseRegistrations();
            courseRegistrations.forEach(c -> {
                if (c.getCourse().getId() == courseId) {
                    if (c.getProfessor() != null) { // Professor exists
                        Professor prof = em.find(Professor.class, c.getProfessor().getId());
                        prof.setProfessor(newProfessor.getFirstName(),
                        				  newProfessor.getLastName(),
                        				  newProfessor.getDepartment());
                        em.merge(prof);
                    }
                    else { // Professor does not exist
                        c.setProfessor(newProfessor);
                        em.merge(studentToBeUpdated);
                    }
                }
            });
            return newProfessor;
        }
        else return null;  // Student doesn't exists
    }

    /**
     * To update a student
     * 
     * @param id - id of entity to update
     * @param studentWithUpdates - entity with updated information
     * @return Entity with updated information
     */
    @Transactional
    public Student updateStudentById(int id, Student studentWithUpdates) {
        Student studentToBeUpdated = getStudentById(id);
        if (studentToBeUpdated != null) {
            em.refresh(studentToBeUpdated);
            em.merge(studentWithUpdates);
            em.flush();
        }
        return studentToBeUpdated;
    }

    /**
     * To delete a student by id
     * 
     * @param id - student id to delete
     */
    @Transactional
    public void deleteStudentById(int id) {
        Student student = getStudentById(id);
        if (student != null) {
            em.refresh(student);
            TypedQuery<SecurityUser> findUser = em.createNamedQuery(SecurityUser.SECURITY_USER_BY_STUDENT_ID_QUERY, SecurityUser.class)
                .setParameter(PARAM1, id);
            SecurityUser sUser = findUser.getSingleResult();
            em.remove(sUser);
            em.remove(student);
        }
    }
    
    public List<StudentClub> getAllStudentClubs() {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<StudentClub> cq = cb.createQuery(StudentClub.class);
        cq.select(cq.from(StudentClub.class));
        return em.createQuery(cq).getResultList();
    }

    // Why not use the build-in em.find?  The named query SPECIFIC_STUDENT_CLUB_QUERY_NAME
    // includes JOIN FETCH that we cannot add to the above API
    public StudentClub getStudentClubById(int id) {
        TypedQuery<StudentClub> specificStudentClubQuery = em.createNamedQuery(SPECIFIC_STUDENT_CLUB_QUERY_NAME, StudentClub.class);
        specificStudentClubQuery.setParameter(PARAM1, id);
        return specificStudentClubQuery.getSingleResult();
    }
    
    // These methods are more generic.

    public <T> List<T> getAll(Class<T> entity, String namedQuery) {
        TypedQuery<T> allQuery = em.createNamedQuery(namedQuery, entity);
        return allQuery.getResultList();
    }
    
    public <T> T getById(Class<T> entity, String namedQuery, int id) {
        TypedQuery<T> allQuery = em.createNamedQuery(namedQuery, entity);
        allQuery.setParameter(PARAM1, id);
        return allQuery.getSingleResult();
    }

    @Transactional
    public StudentClub deleteStudentClub(int id) {
        //StudentClub sc = getStudentClubById(id);
    	StudentClub sc = getById(StudentClub.class, StudentClub.SPECIFIC_STUDENT_CLUB_QUERY_NAME, id);
        if (sc != null) {
            Set<ClubMembership> memberships = sc.getClubMemberships();
            List<ClubMembership> list = new LinkedList<>();
            memberships.forEach(list::add);
            list.forEach(m -> {
                if (m.getCard() != null) {
                    MembershipCard mc = getById(MembershipCard.class, MembershipCard.ID_CARD_QUERY_NAME, m.getCard().getId());
                    mc.setClubMembership(null);
                }
                m.setCard(null);
                em.merge(m);
            });
            em.remove(sc);
            return sc;
        }
        return null;
    }
    
    // Please study & use the methods below in your test suites
    
    public boolean isDuplicated(StudentClub newStudentClub) {
        TypedQuery<Long> allStudentClubsQuery = em.createNamedQuery(IS_DUPLICATE_QUERY_NAME, Long.class);
        allStudentClubsQuery.setParameter(PARAM1, newStudentClub.getName());
        return (allStudentClubsQuery.getSingleResult() >= 1);
    }

    @Transactional
    public StudentClub persistStudentClub(StudentClub newStudentClub) {
        em.persist(newStudentClub);
        return newStudentClub;
    }

    @Transactional
    public StudentClub updateStudentClub(int id, StudentClub updatingStudentClub) {
    	StudentClub studentClubToBeUpdated = getStudentClubById(id);
        if (studentClubToBeUpdated != null) {
            em.refresh(studentClubToBeUpdated);
            studentClubToBeUpdated.setName(updatingStudentClub.getName());
            em.merge(studentClubToBeUpdated);
            em.flush();
        }
        return studentClubToBeUpdated;
    }
    
    @Transactional
    public ClubMembership persistClubMembership(ClubMembership newClubMembership) {
        em.persist(newClubMembership);
        return newClubMembership;
    }

    public ClubMembership getClubMembershipById(int cmId) {
        TypedQuery<ClubMembership> allClubMembershipQuery = em.createNamedQuery(ClubMembership.FIND_BY_ID, ClubMembership.class);
        allClubMembershipQuery.setParameter(PARAM1, cmId);
        return allClubMembershipQuery.getSingleResult();
    }

    @Transactional
    public ClubMembership updateClubMembership(int id, ClubMembership clubMembershipWithUpdates) {
    	ClubMembership clubMembershipToBeUpdated = getClubMembershipById(id);
        if (clubMembershipToBeUpdated != null) {
            em.refresh(clubMembershipToBeUpdated);
            em.merge(clubMembershipWithUpdates);
            em.flush();
        }
        return clubMembershipToBeUpdated;
    }
    
    // Professor methods
    
    public List<Professor> getAllProfessors() {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Professor> cq = cb.createQuery(Professor.class);
        cq.select(cq.from(Professor.class));
        return em.createQuery(cq).getResultList();
    }
    
    public Professor getProfessorById(int id) {
        return em.find(Professor.class, id);
    }
    
    @Transactional
    public Professor persistProfessor(Professor newProfessor) {
        em.persist(newProfessor);
        return newProfessor;
    }
    
    @Transactional
    public Professor updateProfessorById(int id, Professor professorWithUpdates) {
        Professor professorToBeUpdated = getProfessorById(id);
        if (professorToBeUpdated != null) {
            em.refresh(professorToBeUpdated);
            professorToBeUpdated.setProfessor(professorWithUpdates.getFirstName(),
                                               professorWithUpdates.getLastName(),
                                               professorWithUpdates.getDepartment());
            em.merge(professorToBeUpdated);
            em.flush();
        }
        return professorToBeUpdated;
    }
    
    @Transactional
    public void deleteProfessorById(int id) {
        Professor professor = getProfessorById(id);
        if (professor != null) {
            em.refresh(professor);
            em.remove(professor);
        }
    }
    
    // Course methods
    
    public List<Course> getAllCourses() {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Course> cq = cb.createQuery(Course.class);
        cq.select(cq.from(Course.class));
        return em.createQuery(cq).getResultList();
    }
    
    public Course getCourseById(int id) {
        return em.find(Course.class, id);
    }
    
    @Transactional
    public Course persistCourse(Course newCourse) {
        em.persist(newCourse);
        return newCourse;
    }
    
    @Transactional
    public Course updateCourseById(int id, Course courseWithUpdates) {
        Course courseToBeUpdated = getCourseById(id);
        if (courseToBeUpdated != null) {
            em.refresh(courseToBeUpdated);
            courseToBeUpdated.setCourse(courseWithUpdates.getCourseCode(),
                                         courseWithUpdates.getCourseTitle(),
                                         courseWithUpdates.getYear(),
                                         courseWithUpdates.getSemester(),
                                         courseWithUpdates.getCreditUnits(),
                                         courseWithUpdates.getOnline());
            em.merge(courseToBeUpdated);
            em.flush();
        }
        return courseToBeUpdated;
    }
    
    @Transactional
    public void deleteCourseById(int id) {
        Course course = getCourseById(id);
        if (course != null) {
            em.refresh(course);
            em.remove(course);
        }
    }
    
    // MembershipCard methods
    
    public List<MembershipCard> getAllMembershipCards() {
        TypedQuery<MembershipCard> allCardsQuery = em.createNamedQuery(MembershipCard.ALL_CARDS_QUERY_NAME, MembershipCard.class);
        return allCardsQuery.getResultList();
    }
    
    public MembershipCard getMembershipCardById(int id) {
        TypedQuery<MembershipCard> cardQuery = em.createNamedQuery(MembershipCard.ID_CARD_QUERY_NAME, MembershipCard.class);
        cardQuery.setParameter(PARAM1, id);
        try {
            return cardQuery.getSingleResult();
        } catch (Exception e) {
            return null;
        }
    }
    
    @Transactional
    public MembershipCard persistMembershipCard(MembershipCard newMembershipCard) {
        em.persist(newMembershipCard);
        return newMembershipCard;
    }
    
    @Transactional
    public MembershipCard updateMembershipCardById(int id, MembershipCard cardWithUpdates) {
        MembershipCard cardToBeUpdated = getMembershipCardById(id);
        if (cardToBeUpdated != null) {
            em.refresh(cardToBeUpdated);
            cardToBeUpdated.setSigned(cardWithUpdates.getSigned() != 0);
            em.merge(cardToBeUpdated);
            em.flush();
        }
        return cardToBeUpdated;
    }
    
    @Transactional
    public void deleteMembershipCardById(int id) {
        MembershipCard card = getMembershipCardById(id);
        if (card != null) {
            em.refresh(card);
            em.remove(card);
        }
    }
    
    // ClubMembership additional methods
    
    public List<ClubMembership> getAllClubMemberships() {
        TypedQuery<ClubMembership> allClubMembershipQuery = em.createNamedQuery(ClubMembership.FIND_ALL, ClubMembership.class);
        return allClubMembershipQuery.getResultList();
    }
    
    @Transactional
    public void deleteClubMembershipById(int id) {
        ClubMembership clubMembership = getClubMembershipById(id);
        if (clubMembership != null) {
            em.refresh(clubMembership);
            em.remove(clubMembership);
        }
    }
    
    // CourseRegistration methods
    
    public List<CourseRegistration> getAllCourseRegistrations() {
        TypedQuery<CourseRegistration> allQuery = em.createNamedQuery(CourseRegistration.ALL_COURSE_REGISTRATIONS_QUERY, CourseRegistration.class);
        return allQuery.getResultList();
    }
    
    public CourseRegistration getCourseRegistrationById(int studentId, int courseId) {
        CourseRegistrationPK pk = new CourseRegistrationPK();
        pk.setStudentId(studentId);
        pk.setCourseId(courseId);
        return em.find(CourseRegistration.class, pk);
    }
    
    @Transactional
    public CourseRegistration persistCourseRegistration(CourseRegistration newCourseRegistration) {
        em.persist(newCourseRegistration);
        return newCourseRegistration;
    }
    
    @Transactional
    public void deleteCourseRegistrationById(int studentId, int courseId) {
        CourseRegistration courseRegistration = getCourseRegistrationById(studentId, courseId);
        if (courseRegistration != null) {
            em.refresh(courseRegistration);
            em.remove(courseRegistration);
        }
    }
    
}
