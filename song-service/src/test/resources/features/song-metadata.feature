Feature: Song metadata management
  The song service keeps the metadata that was extracted from uploaded MP3 files.
  Every song is identified by the id of the resource it was extracted from.

  Background:
    Given no song metadata is stored

  Scenario: Metadata of a processed resource is stored and can be read back
    When the client submits the metadata of song 1
    Then the response status is 200
    And the response contains the id 1
    When the client requests the metadata of song 1
    Then the response status is 200
    And the song is "Yesterday" by "The Beatles" from the album "Help!"
    And the song lasts "02:05" and was released in "1965"

  Scenario: The same song cannot be stored twice
    Given the metadata of song 5 is already stored
    When the client submits the metadata of song 5
    Then the response status is 409
    And the error message is "Metadata for resource ID=5 already exists"

  Scenario: Metadata that breaks the format rules is rejected
    When the client submits the metadata of song 2 with the duration "2:5"
    Then the response status is 400
    And the error message is "Validation error"
    And the field "duration" is reported as "Duration must be in mm:ss format with leading zeros"

  Scenario: A song that was never stored cannot be found
    When the client requests the metadata of song 42
    Then the response status is 404
    And the error message is "Song metadata for ID=42 not found"

  Scenario: An id that is not a number is rejected
    When the client requests the metadata of song "abc"
    Then the response status is 400
    And the error message is "Invalid value 'abc' for ID. Must be a positive integer"

  Scenario: Deleting songs removes only the ones that exist
    Given the metadata of song 1 is already stored
    And the metadata of song 2 is already stored
    When the client deletes the songs "1,3"
    Then the response status is 200
    And the deleted ids are "1"
    When the client requests the metadata of song 2
    Then the response status is 200
    When the client requests the metadata of song 1
    Then the response status is 404

  Scenario: Deleting with a malformed id list is rejected
    When the client deletes the songs "1,abc"
    Then the response status is 400
    And the error message is "Invalid ID format: 'abc'. Only positive integers are allowed"

  Scenario: A delete request with an oversized id list is rejected
    When the client deletes a list of 120 song ids
    Then the response status is 400
    And the error message starts with "CSV string is too long"
