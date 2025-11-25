/**
 * File:  ClubMembershipResource.java Course materials (23W) CST 8277
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
import static acmecollege.utility.MyConstants.CLUB_MEMBERSHIP_RESOURCE_NAME;
import static acmecollege.utility.MyConstants.RESOURCE_PATH_ID_ELEMENT;
import static acmecollege.utility.MyConstants.RESOURCE_PATH_ID_PATH;

import java.util.List;

import javax.annotation.security.RolesAllowed;
import javax.ejb.EJB;
import javax.inject.Inject;
import javax.security.enterprise.SecurityContext;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import acmecollege.ejb.ACMECollegeService;
import acmecollege.entity.ClubMembership;

@Path(CLUB_MEMBERSHIP_RESOURCE_NAME)
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class ClubMembershipResource {

    private static final Logger LOG = LogManager.getLogger();

    @EJB
    protected ACMECollegeService service;

    @Inject
    protected SecurityContext sc;

    @GET
    @RolesAllowed({ADMIN_ROLE, USER_ROLE})
    public Response getAllClubMemberships() {
        LOG.debug("Retrieving all club memberships...");
        List<ClubMembership> clubMemberships = service.getAllClubMemberships();
        Response response = Response.ok(clubMemberships).build();
        return response;
    }

    @GET
    @RolesAllowed({ADMIN_ROLE, USER_ROLE})
    @Path(RESOURCE_PATH_ID_PATH)
    public Response getClubMembershipById(@PathParam(RESOURCE_PATH_ID_ELEMENT) int membershipId) {
        LOG.debug("Retrieving club membership with id = {}", membershipId);
        ClubMembership clubMembership = service.getClubMembershipById(membershipId);
        Response response = Response.status(clubMembership == null ? Status.NOT_FOUND : Status.OK).entity(clubMembership).build();
        return response;
    }

    @POST
    @RolesAllowed({ADMIN_ROLE})
    public Response addClubMembership(ClubMembership newClubMembership) {
        LOG.debug("Adding a new club membership = {}", newClubMembership);
        ClubMembership clubMembership = service.persistClubMembership(newClubMembership);
        Response response = Response.ok(clubMembership).build();
        return response;
    }

    @PUT
    @RolesAllowed({ADMIN_ROLE})
    @Path(RESOURCE_PATH_ID_PATH)
    public Response updateClubMembership(@PathParam(RESOURCE_PATH_ID_ELEMENT) int membershipId, ClubMembership updatedMembership) {
        LOG.debug("Updating club membership with id = {}", membershipId);
        ClubMembership clubMembership = service.updateClubMembership(membershipId, updatedMembership);
        Response response = Response.status(clubMembership == null ? Status.NOT_FOUND : Status.OK).entity(clubMembership).build();
        return response;
    }

    @DELETE
    @RolesAllowed({ADMIN_ROLE})
    @Path(RESOURCE_PATH_ID_PATH)
    public Response deleteClubMembership(@PathParam(RESOURCE_PATH_ID_ELEMENT) int membershipId) {
        LOG.debug("Deleting club membership with id = {}", membershipId);
        service.deleteClubMembershipById(membershipId);
        Response response = Response.noContent().build();
        return response;
    }
}
