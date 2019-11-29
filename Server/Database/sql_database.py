import sqlite3
import json
from sqlite3 import Error

# Many thanks to https://www.sqlitetutorial.net/sqlite-python/ for basis of functions to interact with
# the database
class Database:
    def __init__(self, file):
        self.conn = self.create_connection(file)
        print(self.conn)

    def create_user(self, user):
        """
        Create a new user into the users table
        :param conn:
        :param user: (int, string,string,string,int)
        :return: project id
        """
        sql = ''' INSERT INTO users(userID,username,saltValue,password,developer)
                VALUES(?,?,?,?,?) '''
        cur = self.conn.cursor()
        cur.execute(sql, user)
        return cur.lastrowid

    def get_user(self, username):
        """
        Query tasks by username
        :param conn: the Connection object
        :param username:
        :return:
        """
        cur = self.conn.cursor()
        cur.execute("SELECT * FROM users WHERE username=?", (username,))
    
        row = cur.fetchone()
        print(row)

        return row
    
    def insert_event(self, event):
        """
        Create a new task
        :param conn:
        :param event: (int,json string,int,int)
        :return:
        """
    
        sql = ''' INSERT INTO MLResults(userID,inputMatrix,prediction,class)
                VALUES(?,?,?,?) '''
        cur = self.conn.cursor()
        cur.execute(sql, event)
        return cur.lastrowid

    def get_user_len(self):
        """
        Query all the rows in the users table and return the len
        :return: The length of users table
        """
        cur = self.conn.cursor()
        cur.execute("SELECT * FROM users")
    
        rows = cur.fetchall()
    
        return len(rows)

    def get_all_events(self):
        """
        Query all rows in the MLResults table that have a valid classification
        :param conn: the Connection object
        :return:
        """
        cur = self.conn.cursor()
        cur.execute("SELECT * FROM MLResults WHERE class IS NOT 0")
    
        rows = cur.fetchall()
    
        for row in rows:
            print(row)
        return rows

    def get_user_events(self, username):
        """
        Query for MLResults table for a specific user
        :param username: The username to filter the results on
        """
        user_id = self.get_user(username)[0] # First element of tuple is user ID
        cur = self.conn.cursor()
        cur.execute("SELECT * FROM MLResults WHERE userID=? AND class IS NOT 0", (user_id,))

        rows = cur.fetchall()

        return rows

    def create_connection(self, db_file):
        """ create a database connection to the SQLite database
            specified by db_file
        :param db_file: database file
        :return: Connection object or None
        """
        conn = None
        try:
            conn = sqlite3.connect(db_file, isolation_level=None)
            print(sqlite3.version)
        except Error as e:
            print(e)
    
        return conn
