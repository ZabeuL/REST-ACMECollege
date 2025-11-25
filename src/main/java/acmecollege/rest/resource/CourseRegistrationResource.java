/**
 * File:  CourseRegistrationResource.java Course materials (23W) CST 8277
 *
 * @author Teddy Yap
 * @author Shariar (Shawn) Emami
 * @author (original) Mike Norman
 * 
 * Updated by:  Group NN
 *   Lucas, Subhechha, David, Abhiram
 * 
 */
package acmecollege.rest.resource;

import static acmecollege.utility.MyConstants.ADMIN_ROLE;
import static acmecollege.utility.MyConstants.USER_ROLE;
import static acmecollege.utility.MyConstants.COURSE_REGISTRATION_RESOURCE_NAME;

import java.util.List;

import javax.annotation.security.RolesAllowed;
import javax.ejb.EJB;
import javax.inject.Inject;
import javax.security.enterprise.SecurityContext;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import acmecollege.ejb.ACMECollegeService;
import acmecollege.entity.CourseRegistration;

@Path(COURSE_REGISTRATION_RESOURCE_NAME)
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class CourseRegistrationResource {

    private static final Logger LOG = LogManager.getLogger();

    @EJB
    protected ACMECollegeService service;

    @Inject
    protected SecurityContext sc;

    @GET
    @RolesAllowed({ADMIN_ROLE, USER_ROLE})
    public Response getAllCourseRegistrations() {
        LOG.debug("Retrieving all course registrations...");
        List<CourseRegistration> courseRegistrations = service.getAllCourseRegistrations();
        Response response = Response.ok(courseRegistrations).build();
        return response;
    }

    @GET
    @RolesAllowed({ADMIN_ROLE, USER_ROLE})
    @Path("/{studentId}/{courseId}")
    public Response getCourseRegistrationById(@PathParam("studentId") int studentId, @PathParam("courseId") int courseId) {
        LOG.debug("Retrieving course registration with studentId = {} and courseId = {}", studentId, courseId);
        CourseRegistration courseRegistration = service.getCourseRegistrationById(studentId, courseId);
        Response response = Response.status(courseRegistration == null ? Status.NOT_FOUND : Status.OK).entity(courseRegistration).build();
        return response;
    }

    @POST
    @RolesAllowed({ADMIN_ROLE})
    public Response addCourseRegistration(CourseRegistration newCourseRegistration) {
        LOG.debug("Adding a new course registration = {}", newCourseRegistration);
        CourseRegistration courseRegistration = service.persistCourseRegistration(newCourseRegistration);
        Response response = Response.ok(courseRegistration).build();
        return response;
    }

    @DELETE
    @RolesAllowed({ADMIN_ROLE})
    @Path("/{studentId}/{courseId}")
    public Response deleteCourseRegistration(@PathParam("studentId") int studentId, @PathParam("courseId") int courseId) {
        LOG.debug("Deleting course registration with studentId = {} and courseId = {}", studentId, courseId);
        service.deleteCourseRegistrationById(studentId, courseId);
        Response response = Response.noContent().build();
        return response;
    }
}
