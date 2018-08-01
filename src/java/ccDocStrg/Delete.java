package ccDocStrg;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.FilterOperator;
import com.google.appengine.api.datastore.Query.FilterPredicate;
import com.google.appengine.tools.cloudstorage.GcsFilename;
import com.google.appengine.tools.cloudstorage.GcsService;
import com.google.appengine.tools.cloudstorage.GcsServiceFactory;
import com.google.appengine.tools.cloudstorage.ListItem;
import com.google.appengine.tools.cloudstorage.ListOptions;
import com.google.appengine.tools.cloudstorage.ListResult;
import com.google.appengine.tools.cloudstorage.RetryParams;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 *
 * @author Muhammad Wannous This Servlet accepts one parameter for the file name
 * and deletes its name from the Datastore (the actual file is not deleted)
 */
public class Delete extends HttpServlet {

    /**
     * Processes requests for both HTTP <code>GET</code> and <code>POST</code>
     * methods.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        //Prepare the session context.
        HttpSession session = request.getSession(true);
        //Get the user information from the session context.
        User currentUSer = (User) session.getAttribute(Defs.SESSION_USER_STRING);
        //Get the file name from the URL
        String fileName = request.getParameter(Defs.PARAM_FILENAME_STRING);
        //Make sure that the user has already loggedin and that the fileName parameter is not empty/null.
        if (currentUSer != null
                && fileName != null
                && !fileName.equals("")) {
            //Prepare the Datastore service.
            DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
            //We will serach in the 'Files' table for the file name.
            Query fileQuery = new Query(Defs.DATASTORE_KIND_FILES_STRING);
            //Set a filetr on the file name.
            Query.Filter fileFilter = new Query.FilterPredicate(Defs.ENTITY_PROPERTY_FILENAME_STRING,
                    Query.FilterOperator.EQUAL, fileName);
            fileQuery.setFilter(fileFilter);
            //Run the query.
            List<Entity> dbFiles = datastore.prepare(fileQuery).asList(FetchOptions.Builder.withDefaults());
            if (!dbFiles.isEmpty()) {
                //If the file name was found then delete it from the Datastore.
                //Marius
                this.doBackUp(fileName, currentUSer.getUserName());
                datastore.delete(dbFiles.get(0).getKey());
                session.setAttribute(Defs.SESSION_MESSAGE_STRING, "The file indicated was deleted!");
                response.sendRedirect(Defs.LIST_PAGE_STRING);
            } else {
                //There was no such file name.
                session.setAttribute(Defs.SESSION_MESSAGE_STRING, "No such file!");
                response.sendRedirect(Defs.LIST_PAGE_STRING);
            }
        } else {
            //If the user has not logged in then return him/her to the login page.
            session.setAttribute(Defs.SESSION_MESSAGE_STRING, "Please login firt!");
            response.sendRedirect(Defs.LOGIN_PAGE_STRING);
        }
    }

    // <editor-fold defaultstate="collapsed" desc="HttpServlet methods. Click on the + sign on the left to edit the code.">
    /**
     * Handles the HTTP <code>GET</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    /**
     * Handles the HTTP <code>POST</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    /**
     * Returns a short description of the servlet.
     *
     * @return a String containing servlet description
     */
    @Override
    public String getServletInfo() {
        return "Short description";
    }// </editor-fold>

    //Storage clean up
    //Delete the files in the storage when they are not listed in the Datastore after a period of time from deletion
    public static void doCleanUp(String userName) throws Exception {
        String bucket = Defs.BUCKET_STRING;
        try {

            //Prepare the Datastore service.
            DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();

            Query.Filter propertyFilter = new FilterPredicate(Defs.ENTITY_PROPERTY_UPLOADER_STRING, FilterOperator.EQUAL, userName);
            Query fileQuery = new Query(Defs.DATASTORE_KIND_FILES_BACKUP_STRING).setFilter(propertyFilter);
            //Run the query.
            List<Entity> dbFiles = datastore.prepare(fileQuery).asList(FetchOptions.Builder.withDefaults());

            if (!dbFiles.isEmpty()) {

                Iterator<Entity> allFiles = dbFiles.iterator();

                while (allFiles.hasNext()) {
                    String path = (String) allFiles.next().getProperty(Defs.ENTITY_PROPERTY_PATH_STRING);
                    String time = (String) allFiles.next().getProperty(Defs.ENTITY_PROPERTY_DELETED_TIME_STRING);
                    //Prepare the GCS service.
                    GcsService gcsService = GcsServiceFactory.createGcsService(new RetryParams.Builder()
                            .initialRetryDelayMillis(10)
                            .retryMaxAttempts(10)
                            .totalRetryPeriodMillis(15000)
                            .build());
                    //Get list in the folder
                    ListResult list = gcsService.list(bucket, new ListOptions.Builder().setPrefix(userName).setRecursive(true).build());
                    while (list.hasNext()) {
                        ListItem item = list.next();
                        if (Delete.doTimeDifference(time) > 180) {
                            String deletedTime = Long.toString(Delete.doTimeDifference(time));
                            //session.setAttribute(Defs.SESSION_MESSAGE_STRING, deletedTime);
                            
                            gcsService.delete(new GcsFilename(bucket, path));
                            datastore.delete(dbFiles.get(0).getKey());
                            //gcsService.delete(new GcsFilename(bucket, item.getName()));
                        }
                    }
                }
            }
        } catch (IOException e) {
            //Error handling
        }
    }

    //Function to calculate the difference between two strings of time
    public static long doTimeDifference(String time) throws Exception {
        String time1 = time;
        String time2 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());

        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date date1 = format.parse(time1);
        Date date2 = format.parse(time2);
        long difference = date2.getTime() - date1.getTime();

        //the different is in milliseconds, it needs to be divided by 1000 to get the number of seconds
        return difference / 1000;
    }

    public void doBackUp(String fileName, String userName) {
        String timeStamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
        //We will serach in the 'Files' table for the file name.
        Entity fileEntity = new Entity(Defs.DATASTORE_KIND_FILES_BACKUP_STRING);
        fileEntity.setProperty(Defs.ENTITY_PROPERTY_FILENAME_STRING, fileName);
        fileEntity.setProperty(Defs.ENTITY_PROPERTY_UPLOADER_STRING, userName);
        fileEntity.setProperty(Defs.ENTITY_PROPERTY_DELETED_TIME_STRING, timeStamp);
        fileEntity.setProperty(Defs.ENTITY_PROPERTY_PATH_STRING, userName + "/" + fileName);
        //No need for filters.
        datastore.put(fileEntity);
    }
}
