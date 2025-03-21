// SPDX-License-Identifier: MIT
pragma solidity ^0.8.22;

interface IAccessControl {
    function addToBlacklist(address) external returns (bool);
    function removeFromBlacklist(address) external returns (bool);
    function isBlacklisted(address) external view returns (bool);
}

contract AccessControl is IAccessControl {

    address public owner;
    mapping(address => bool) private blacklisted;
    mapping(address => bool) public authorizedParties;

    modifier onlyOwner() {
        require(msg.sender == owner, "Not authorized (only owner)");
        _;
    }

    modifier onlyAuthorized() {
        require(authorizedParties[msg.sender] == true, "Not authorized");
        _;
    }

    constructor() {
        owner = msg.sender;
        authorizedParties[owner] = true; // only owner is initially authorized
    }

    function addToBlacklist(address account) public onlyAuthorized returns (bool) {
        require(account != address(0), "Invalid address");
        require(!blacklisted[account], "Already blacklisted");

        blacklisted[account] = true;
        return true;
    }

    function removeFromBlacklist(address account) public onlyAuthorized returns (bool) {
        require(account != address(0), "Invalid address");
        require(blacklisted[account], "Not blacklisted");

        blacklisted[account] = false;
        return true;
    }

    function isBlacklisted(address account) public view returns (bool) {
        return blacklisted[account];
    }

    // only owner can add or remove authorized parties
    function setAuthorizedParty(address party, bool status) public onlyOwner {
        authorizedParties[party] = status;
    }
}
